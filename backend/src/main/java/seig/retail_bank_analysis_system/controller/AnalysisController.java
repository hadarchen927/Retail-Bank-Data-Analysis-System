package seig.retail_bank_analysis_system.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import seig.retail_bank_analysis_system.util.Log;
import seig.retail_bank_analysis_system.util.Result;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // 静态变量暂存最新的模型评价指标
    private static Map<String, Object> lastMetrics = new HashMap<>();

    static {
        lastMetrics.put("sse", 0.0);
        lastMetrics.put("sil", 0.0);
        lastMetrics.put("k", 3);
    }

    /**
     * 执行 K-Means 聚类训练
     */
    @Log("执行K-Means聚类分析模型训练")
    @PostMapping("/run")
    public Result<?> runAnalysis(@RequestParam(defaultValue = "3") Integer k) {
        String folderPath = "D:/retail-bank-analysis-system/data/";
        String fileName = folderPath + "temp_analysis.xlsx";
        String pythonExe = "D:/Anaconda3/python.exe";
        String scriptPath = "D:/retail-bank-analysis-system/backend/python/cluster_analysis.py";

        try {
            Files.createDirectories(Paths.get(folderPath));

            List<BankCustomer> list = customerMapper.selectList(null);
            if (list.isEmpty()) return Result.error("客户库无数据，请先导入");

            // 1. 导出数据供 Python 脚本读取
            EasyExcel.write(fileName, BankCustomer.class).sheet("分析数据").doWrite(list);

            // 2. 调用 Python 并传入路径和 K 值
            ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath, fileName, k.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 3. 读取 Python 控制台输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line);
                System.out.println("AI引擎输出: " + line);
            }

            int exitCode = process.waitFor();
            String outStr = output.toString();

            // 4. 解析算法回传的评估指标
            // 期待格式: SUCCESS:K:3:SCORE:123.45:SILHOUETTE:0.67
            if (exitCode == 0 && outStr.contains("SUCCESS")) {
                String[] parts = outStr.split(":");
                if (parts.length >= 7) {
                    try {
                        lastMetrics.put("k", Integer.parseInt(parts[2]));
                        lastMetrics.put("sse", Double.parseDouble(parts[4]));
                        lastMetrics.put("sil", Double.parseDouble(parts[6]));
                    } catch (Exception e) {
                        System.err.println("指标解析异常: " + e.getMessage());
                    }
                }

                // 5. 将 Python 计算后的结果写回数据库
                List<BankCustomer> resultList = EasyExcel.read(fileName).head(BankCustomer.class).sheet().doReadSync();
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

    /**
     * 获取特征解读摘要（对应前端下方表格）
     */
    @GetMapping("/summary")
    public Result<?> getClusterSummary() {
        // 1. 聚合统计
        List<Map<String, Object>> summary = customerMapper.selectMaps(new QueryWrapper<BankCustomer>()
                .select("cluster_tag",
                        "AVG(annual_income) as avgIncome",
                        "AVG(total_assets) as avgAssets",
                        "COUNT(*) as count")
                .groupBy("cluster_tag")
                .having("cluster_tag >= 0")
                .orderByAsc("cluster_tag"));

        // 2. 语义化标签判断
        for (Map<String, Object> map : summary) {
            double avgInc = ((Number) map.get("avgIncome")).doubleValue();
            double avgAst = ((Number) map.get("avgAssets")).doubleValue();

            // 四舍五入处理，让前端显示更漂亮
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

    /**
     * 获取评分（用于页面初始化）
     */
    @GetMapping("/score")
    public Result<?> getScore() {
        return Result.success(lastMetrics);
    }

    /**
     * 获取散点图原始数据
     */
    @GetMapping("/chart-data")
    public Result<?> getChartData() {
        return Result.success(customerMapper.selectList(null));
    }

    /**
     * 重置所有模型数据
     */
    @Log("重置AI分群模型结果")
    @PostMapping("/reset")
    public Result<?> resetClusters() {
        BankCustomer customer = new BankCustomer();
        customer.setClusterTag(-1);
        customerMapper.update(customer, new QueryWrapper<>());

        lastMetrics.put("sse", 0.0);
        lastMetrics.put("sil", 0.0);
        lastMetrics.put("k", 3);

        return Result.success("分群模型已重置，数据库已同步");
    }
}