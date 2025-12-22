package seig.retail_bank_analysis_system.controller;

import com.alibaba.excel.EasyExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import seig.retail_bank_analysis_system.util.Log;
import seig.retail_bank_analysis_system.util.Result;
import java.io.*;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private BankCustomerMapper customerMapper;

    // 使用静态变量记录最近一次运行的 SSE 分数，防止刷新页面后分数变为 0
    private static double lastSseScore = 0.0;

    @Log("执行K-Means聚类分析")
    @PostMapping("/run")
    public Result<?> runAnalysis() {
        String folderPath = "D:/retail-bank-analysis-system/data/";
        String fileName = folderPath + "temp_analysis.xlsx";
        String pythonExe = "D:/Anaconda3/python.exe";
        String scriptPath = "D:/retail-bank-analysis-system/backend/python/cluster_analysis.py";

        try {
            Files.createDirectories(Paths.get(folderPath));

            List<BankCustomer> list = customerMapper.selectList(null);
            if (list.isEmpty()) return Result.error("数据库为空，请先导入数据");

            // 1. 将数据写入 Excel 给 Python 读取
            EasyExcel.write(fileName, BankCustomer.class).sheet("分析数据").doWrite(list);

            // 2. 启动 Python 进程
            ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath, fileName);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 3. 读取 Python 控制台的所有输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line);
                System.out.println("Python输出: " + line); // 在后端控制台打印，方便调试
            }

            int exitCode = process.waitFor();
            String outStr = output.toString();

            // 4. 判断是否运行成功并解析分数
            if (exitCode == 0 && outStr.contains("SUCCESS")) {

                // 【核心逻辑】从输出中提取分数：假设格式为 SUCCESS:SCORE:1234.56
                if (outStr.contains("SCORE:")) {
                    String[] parts = outStr.split(":");
                    // parts[2] 就是 SSE 分数
                    lastSseScore = Double.parseDouble(parts[2]);
                    // 如果你想显示轮廓系数，可以再定义一个静态变量存 parts[4]
                }

                // 5. 算法跑完后，Java 读回 Excel 并更新数据库标签
                List<BankCustomer> resultList = EasyExcel.read(fileName).head(BankCustomer.class).sheet().doReadSync();
                for (BankCustomer customer : resultList) {
                    if (customer.getId() != null) {
                        BankCustomer updateObj = new BankCustomer();
                        updateObj.setId(customer.getId());
                        updateObj.setClusterTag(customer.getClusterTag());
                        customerMapper.updateById(updateObj);
                    }
                }

                // 返回成功，并把最新的分数带给前端
                return Result.success(lastSseScore);
            } else {
                return Result.error("Python 算法报错: " + outStr);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近一次的模型评估分数
     * 用于前端页面初始化时显示
     */
    @GetMapping("/score")
    public Result<?> getScore() {
        return Result.success(lastSseScore);
    }

    /**
     * 获取散点图数据
     */
    @GetMapping("/chart-data")
    public Result<?> getChartData() {
        List<BankCustomer> list = customerMapper.selectList(null);
        return Result.success(list);
    }

    /**
     * 重置所有分群标签
     */
    @Log("重置聚类结果")
    @PostMapping("/reset")
    public Result<?> resetClusters() {
        BankCustomer customer = new BankCustomer();
        customer.setClusterTag(-1);
        customerMapper.update(customer, null);
        lastSseScore = 0.0; // 重置分数
        return Result.success("重置成功");
    }
}