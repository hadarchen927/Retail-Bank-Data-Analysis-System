package seig.retail_bank_analysis_system.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.dao.BankTransactionMapper;
import seig.retail_bank_analysis_system.dao.RiskRecordMapper;
import seig.retail_bank_analysis_system.dto.CustomerQueryDTO;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import seig.retail_bank_analysis_system.entity.BankTransaction;
import seig.retail_bank_analysis_system.util.CustomerDataListener;
import seig.retail_bank_analysis_system.util.Log;
import seig.retail_bank_analysis_system.util.Result;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataManagementController {

    @Autowired
    private BankCustomerMapper customerMapper;

    @Autowired
    private BankTransactionMapper transactionMapper;

    @Autowired
    private RiskRecordMapper riskRecordMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ================= 1. 客户基础数据模块 (CSV) =================

    /**
     * 获取所有客户列表
     */
    @GetMapping("/list")
    public Result<?> getCustomerList() {
        return Result.success(customerMapper.selectList(null));
    }

    /**
     * 导入客户基础数据 CSV
     */
    @Log("导入客户基础数据(CSV)")
    @PostMapping("/import")
    public Result<?> importCustomers(@RequestParam("file") MultipartFile file) throws IOException {
        CustomerDataListener listener = new CustomerDataListener(customerMapper);

        // 【关键修改】指定 excelType 为 CSV
        EasyExcel.read(file.getInputStream(), BankCustomer.class, listener)
                .excelType(ExcelTypeEnum.CSV)
                .charset(StandardCharsets.UTF_8)
                .sheet()
                .doRead();

        // 数据变动，清除缓存
        redisTemplate.delete("dashboard_data_cache");

        String msg = String.format("CSV客户数据导入成功！入库：%d 条，剔除脏数据：%d 条",
                listener.getSuccessCount(), listener.getSkipCount());
        return Result.success(msg);
    }


    // ================= 2. 交易流水数据模块 (CSV) =================

    /**
     * 获取交易流水列表
     */
    @GetMapping("/transaction-list")
    public Result<?> getTransactionList() {
        return Result.success(transactionMapper.selectList(new LambdaQueryWrapper<BankTransaction>()
                .orderByDesc(BankTransaction::getTransTime)
                .last("LIMIT 100")));
    }

    /**
     * 导入交易流水数据 CSV
     */
    @Log("导入交易流水(CSV)并执行清洗")
    @PostMapping("/import-transactions")
    public Result<?> importTransactions(@RequestParam("file") MultipartFile file) throws IOException {
        // 【关键修改】指定 excelType 为 CSV
        List<BankTransaction> list = EasyExcel.read(file.getInputStream())
                .excelType(ExcelTypeEnum.CSV)
                .charset(StandardCharsets.UTF_8)
                .head(BankTransaction.class)
                .sheet()
                .doReadSync();

        int cleanCount = 0;
        for (BankTransaction trans : list) {
            // 流水清洗规则
            if (trans.getAmount() != null && trans.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                trans.setAmount(trans.getAmount().abs());
                cleanCount++;
            }
            if (trans.getTransTime() == null) {
                trans.setTransTime(java.time.LocalDateTime.now());
            }
            trans.setId(null);
            transactionMapper.insert(trans);
        }

        redisTemplate.delete("dashboard_data_cache");
        return Result.success("成功导入 " + list.size() + " 条记录，清洗修正 " + cleanCount + " 项");
    }

    // ================= 3. 高级交叉分析模块 =================

    @PostMapping("/search")
    public Result<?> searchCustomers(@RequestBody CustomerQueryDTO query) {
        List<Map<String, Object>> result = customerMapper.crossAnalyze(query);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPeople", result.size());

        double avgTrans = result.stream()
                .mapToDouble(m -> {
                    Object amount = m.get("total_trans_amount");
                    if (amount == null) return 0;
                    return (amount instanceof BigDecimal) ?
                            ((BigDecimal) amount).doubleValue() :
                            Double.parseDouble(amount.toString());
                })
                .average().orElse(0);

        summary.put("avgTransAmount", Math.round(avgTrans * 100.0) / 100.0);

        Map<String, Object> response = new HashMap<>();
        response.put("list", result);
        response.put("summary", summary);
        return Result.success(response);
    }

    // ================= 4. 系统管理与导出 (CSV) =================

    /**
     * 一键重置数据
     */
    @Log("清空系统所有业务数据")
    @PostMapping("/clear-all")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> clearAllData() {
        try {
            customerMapper.delete(null);
            transactionMapper.delete(null);
            riskRecordMapper.delete(null);

            jdbcTemplate.execute("ALTER TABLE bank_customer AUTO_INCREMENT = 1");
            jdbcTemplate.execute("ALTER TABLE bank_transaction AUTO_INCREMENT = 1");
            jdbcTemplate.execute("ALTER TABLE bank_risk_record AUTO_INCREMENT = 1");

            redisTemplate.delete("dashboard_data_cache");
            return Result.success("系统数据已重置，ID已归零");
        } catch (Exception e) {
            return Result.error("重置失败：" + e.getMessage());
        }
    }

    /**
     * 导出分析结果为 CSV (解决乱码问题)
     */
    @Log("导出分析结果(CSV)")
    @GetMapping("/export")
    public void exportCustomers(HttpServletResponse response) throws IOException {
        // 1. 设置 Content-Type 为 text/csv
        response.setContentType("text/csv");
        response.setCharacterEncoding("utf-8");

        // 2. 防止中文文件名乱码
        String fileName = URLEncoder.encode("银行客户分析结果报表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".csv");

        // 3. 获取数据
        List<BankCustomer> list = customerMapper.selectList(null);

        // 4. 【核心修复】指定 excelType 为 CSV
        // EasyExcel 在导出 CSV 时会自动写入 BOM (Byte Order Mark)，
        // 从而确保 Windows Excel 打开时能正确识别 UTF-8 编码，不再显示问号。
        EasyExcel.write(response.getOutputStream(), BankCustomer.class)
                .excelType(ExcelTypeEnum.CSV)
                .charset(StandardCharsets.UTF_8)
                .sheet("结果")
                .doWrite(list);
    }
}