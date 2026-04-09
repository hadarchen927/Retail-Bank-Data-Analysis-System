package seig.retail_bank_analysis_system.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.service.BankCustomerService;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import seig.retail_bank_analysis_system.util.JwtUtils;
import seig.retail_bank_analysis_system.util.Log;
import seig.retail_bank_analysis_system.util.Result;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private BankCustomerMapper customerMapper;

    // 【新增】注入 Service 层用于批量更新性能优化
    @Autowired
    private BankCustomerService customerService;

    // 【新增】通过配置文件注入路径，告别硬编码
    @Value("${python.env.exe-path:D:/Anaconda/python.exe}")
    private String pythonExe;

    @Value("${python.env.script-path:D:/retail-bank-analysis-system/backend/python/cluster_analysis.py}")
    private String scriptPath;

    @Value("${python.env.data-folder:D:/retail-bank-analysis-system/data/}")
    private String folderPath;

    private static Map<String, Object> lastMetrics = new HashMap<>();

    static {
        lastMetrics.put("sse", 0.0);
        lastMetrics.put("sil", 0.0);
        lastMetrics.put("k", 3);
        lastMetrics.put("elbowData", new ArrayList<>());
    }

    @Log("执行AI分群模型训练(含肘部评估)")
    @PostMapping("/run")
    public Result<?> runAnalysis(@RequestParam(defaultValue = "3") Integer k, HttpServletRequest request) {

        // --- 后端安全加固 ---
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            try {
                JwtUtils.getUsername(token);
            } catch (Exception e) {
                return Result.error("身份认证失效，请重新登录");
            }
        }

        String fileName = folderPath + "temp_analysis.csv";

        try {
            Files.createDirectories(Paths.get(folderPath));

            List<BankCustomer> list = customerMapper.selectList(null);
            if (list.isEmpty()) return Result.error("数据库无数据，请先导入");

            // 1. 导出 CSV 供 Python 读取
            EasyExcel.write(fileName, BankCustomer.class)
                    .excelType(ExcelTypeEnum.CSV)
                    .charset(StandardCharsets.UTF_8)
                    .sheet("分析数据")
                    .doWrite(list);

            // 2. 调用 Python 引擎并传入 K 值
            ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath, fileName, k.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 3. 【优化】精准读取控制台输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            String line;
            String successLine = null;
            StringBuilder errorLogs = new StringBuilder(); // 收集其他可能报错信息

            while ((line = reader.readLine()) != null) {
                System.out.println("Python引擎输出: " + line);
                if (line.startsWith("SUCCESS:")) {
                    successLine = line; // 命中核心数据行
                } else if (line.startsWith("ERROR:")) {
                    errorLogs.append(line).append(" ");
                }
            }

            int exitCode = process.waitFor();

            // 4. 解析包含 ELBOW 数据的新输出格式
            if (exitCode == 0 && successLine != null) {
                String[] parts = successLine.split(":");

                if (parts.length >= 9) {
                    try {
                        String elbowRaw = parts[2];
                        List<Map<String, Object>> elbowList = new ArrayList<>();
                        for (String pair : elbowRaw.split(",")) {
                            String[] kv = pair.split("_");
                            Map<String, Object> node = new HashMap<>();
                            node.put("k", kv[0]);
                            double sseVal = Double.parseDouble(kv[1]);
                            node.put("sse", Math.round(sseVal * 100.0) / 100.0); // 保留两位小数，提升前端美观度
                            elbowList.add(node);
                        }
                        lastMetrics.put("elbowData", elbowList);
                        lastMetrics.put("k", Integer.parseInt(parts[4]));
                        lastMetrics.put("sse", Double.parseDouble(parts[6]));
                        lastMetrics.put("sil", Double.parseDouble(parts[8]));
                    } catch (Exception e) {
                        System.err.println("解析评估参数出错: " + e.getMessage());
                    }
                }

                // 5. 读回 CSV 更新数据库标签
                List<BankCustomer> resultList = EasyExcel.read(fileName)
                        .excelType(ExcelTypeEnum.CSV)
                        .charset(StandardCharsets.UTF_8)
                        .head(BankCustomer.class)
                        .sheet()
                        .doReadSync();

                // 【优化】MyBatis-Plus 批量更新，性能百倍提升
                List<BankCustomer> updateList = new ArrayList<>();
                for (BankCustomer customer : resultList) {
                    if (customer.getId() != null) {
                        BankCustomer updateObj = new BankCustomer();
                        updateObj.setId(customer.getId());
                        updateObj.setClusterTag(customer.getClusterTag());
                        updateList.add(updateObj);
                    }
                }

                if (!updateList.isEmpty()) {
                    // 调用 IService 的批量更新方法
                    customerService.updateBatchById(updateList);
                }

                return Result.success(lastMetrics);
            } else {
                return Result.error("AI 算法引擎异常: " + (errorLogs.length() > 0 ? errorLogs.toString() : "未知错误"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("算法调度失败: " + e.getMessage());
        }
    }

    @GetMapping("/summary")
    public Result<?> getClusterSummary() {
        List<Map<String, Object>> summary = customerMapper.selectMaps(new QueryWrapper<BankCustomer>()
                .select("cluster_tag",
                        "AVG(annual_income) as avgIncome",
                        "AVG(total_assets) as avgAssets",
                        "COUNT(*) as count")
                .groupBy("cluster_tag")
                .having("cluster_tag >= 0")
                .orderByAsc("cluster_tag"));

        for (Map<String, Object> map : summary) {
            double avgInc = ((Number) map.get("avgIncome")).doubleValue();
            double avgAst = ((Number) map.get("avgAssets")).doubleValue();

            map.put("avgIncome", Math.round(avgInc * 100.0) / 100.0);
            map.put("avgAssets", Math.round(avgAst * 100.0) / 100.0);

            String label;
            if (avgInc > 500000) {
                label = (avgAst > 4000000) ? "顶级私人银行客户" : "高薪高净值客户";
            } else if (avgInc > 200000) {
                label = (avgAst > 2000000) ? "资产稳健中坚客户" : "高成长型中坚客户";
            } else if (avgAst > 1000000) {
                label = "存量潜力基础客户";
            } else {
                label = "大众普惠客户(组" + map.get("cluster_tag") + ")";
            }
            map.put("label", label);
        }
        return Result.success(summary);
    }

    @GetMapping("/score")
    public Result<?> getScore() {
        return Result.success(lastMetrics);
    }

    @GetMapping("/chart-data")
    public Result<?> getChartData() {
        return Result.success(customerMapper.selectList(null));
    }

    @Log("重置AI分群模型结果")
    @PostMapping("/reset")
    public Result<?> resetClusters() {
        BankCustomer customer = new BankCustomer();
        customer.setClusterTag(-1);
        customerMapper.update(customer, new QueryWrapper<>());

        lastMetrics.put("sse", 0.0);
        lastMetrics.put("sil", 0.0);
        lastMetrics.put("k", 3);
        lastMetrics.put("elbowData", new ArrayList<>());

        return Result.success("分群模型已重置");
    }
}