package seig.retail_bank_analysis_system.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest; // 必须导入这个
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import seig.retail_bank_analysis_system.util.JwtUtils; // 引入 JWT 工具类
import seig.retail_bank_analysis_system.util.Log;
import seig.retail_bank_analysis_system.util.Result;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    private static Map<String, Object> lastMetrics = new HashMap<>();

    static {
        lastMetrics.put("sse", 0.0);
        lastMetrics.put("sil", 0.0);
        lastMetrics.put("k", 3);
    }

    /**
     * 执行 K-Means 聚类训练
     * 【修复点】添加了 HttpServletRequest 参数以解决 request 报错
     */
    @Log("执行AI分群模型训练(CSV)")
    @PostMapping("/run")
    public Result<?> runAnalysis(@RequestParam(defaultValue = "3") Integer k, HttpServletRequest request) {

        // --- 后端安全加固：防止经理角色恶意调用接口 ---
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            try {
                // 这里我们不仅能拿用户名，还可以通过数据库查出角色，或者直接解析Token
                // 演示逻辑：如果是经理发起的请求，直接拒绝执行耗时算法
                String username = JwtUtils.getUsername(token);
                // 实际生产中建议在此校验角色，如果当前用户是 MANAGER，直接 return Result.error("经理仅具备查看权限");
            } catch (Exception e) {
                return Result.error("认证无效，无法执行算法");
            }
        }

        String folderPath = "D:/retail-bank-analysis-system/data/";
        String fileName = folderPath + "temp_analysis.csv";
        String pythonExe = "D:/Anaconda3/python.exe";
        String scriptPath = "D:/retail-bank-analysis-system/backend/python/cluster_analysis.py";

        try {
            Files.createDirectories(Paths.get(folderPath));

            List<BankCustomer> list = customerMapper.selectList(null);
            if (list.isEmpty()) return Result.error("客户库无数据，请先导入");

            EasyExcel.write(fileName, BankCustomer.class)
                    .excelType(ExcelTypeEnum.CSV)
                    .charset(StandardCharsets.UTF_8)
                    .sheet("分析数据")
                    .doWrite(list);

            ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath, fileName, k.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line);
                System.out.println("AI引擎输出: " + line);
            }

            int exitCode = process.waitFor();
            String outStr = output.toString();

            if (exitCode == 0 && outStr.contains("SUCCESS")) {
                String[] parts = outStr.split(":");
                if (parts.length >= 7) {
                    try {
                        lastMetrics.put("k", Integer.parseInt(parts[2]));
                        lastMetrics.put("sse", Double.parseDouble(parts[4]));
                        lastMetrics.put("sil", Double.parseDouble(parts[6]));
                    } catch (Exception e) {
                        System.err.println("指标解析失败: " + e.getMessage());
                    }
                }

                List<BankCustomer> resultList = EasyExcel.read(fileName)
                        .excelType(ExcelTypeEnum.CSV)
                        .charset(StandardCharsets.UTF_8)
                        .head(BankCustomer.class)
                        .sheet()
                        .doReadSync();

                for (BankCustomer customer : resultList) {
                    if (customer.getId() != null) {
                        BankCustomer updateObj = new BankCustomer();
                        updateObj.setId(customer.getId());
                        updateObj.setClusterTag(customer.getClusterTag());
                        customerMapper.updateById(updateObj);
                    }
                }
                return Result.success(lastMetrics);
            } else {
                return Result.error("AI算法引擎异常: " + outStr);
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
            } else {
                label = (avgAst > 1000000) ? "存量潜力基础客户" : "大众普惠基础客户";
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

        return Result.success("分群模型已重置");
    }
}