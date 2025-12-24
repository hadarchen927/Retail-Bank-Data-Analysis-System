package seig.retail_bank_analysis_system.controller;

import com.alibaba.excel.EasyExcel;
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

    // ================= 1. 客户基础数据模块 =================

    /**
     * 获取所有客户列表
     */
    @GetMapping("/list")
    public Result<?> getCustomerList() {
        return Result.success(customerMapper.selectList(null));
    }

    /**
     * 导入客户基础数据 Excel
     */
    @Log("导入客户基础数据")
    @PostMapping("/import")
    public Result<?> importCustomers(@RequestParam("file") MultipartFile file) throws IOException {
        EasyExcel.read(file.getInputStream(), BankCustomer.class, new CustomerDataListener(customerMapper)).sheet().doRead();
        // 数据变动，清除缓存
        redisTemplate.delete("dashboard_data_cache");
        return Result.success("客户数据导入成功");
    }


    // ================= 2. 交易流水数据模块 =================

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
     * 导入交易流水数据 Excel
     */
    @Log("导入银行流水数据")
    @PostMapping("/import-transactions")
    public Result<?> importTransactions(@RequestParam("file") MultipartFile file) throws IOException {
        List<BankTransaction> list = EasyExcel.read(file.getInputStream()).head(BankTransaction.class).sheet().doReadSync();
        for (BankTransaction trans : list) {
            trans.setId(null);
            transactionMapper.insert(trans);
        }
        // 数据变动，清除缓存
        redisTemplate.delete("dashboard_data_cache");
        return Result.success("成功导入 " + list.size() + " 条流水数据");
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

    // ================= 4. 系统数据重置模块 =================

    /**
     * 一键清空所有业务数据并重置自增ID
     */
    @Log("清空系统所有业务数据")
    @PostMapping("/clear-all")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> clearAllData() {
        try {
            // 1. 清空三个核心业务表的数据
            customerMapper.delete(null);
            transactionMapper.delete(null);
            riskRecordMapper.delete(null);

            // 2. 重置 MySQL 自增 ID 计数器为 1
            jdbcTemplate.execute("ALTER TABLE bank_customer AUTO_INCREMENT = 1");
            jdbcTemplate.execute("ALTER TABLE bank_transaction AUTO_INCREMENT = 1");
            jdbcTemplate.execute("ALTER TABLE bank_risk_record AUTO_INCREMENT = 1");

            // 3. 强制失效 Redis 缓存
            redisTemplate.delete("dashboard_data_cache");

            return Result.success("系统业务数据已重置，ID 计数器已归零");
        } catch (Exception e) {
            return Result.error("重置失败：" + e.getMessage());
        }
    }

    @Log("导出客户分群数据")
    @GetMapping("/export")
    public void exportCustomers(HttpServletResponse response) throws IOException {
        // 1. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "Bank_Customer_Analysis_Result";
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        // 2. 获取数据
        List<BankCustomer> list = customerMapper.selectList(null);

        // 3. 执行导出
        EasyExcel.write(response.getOutputStream(), BankCustomer.class)
                .sheet("分群结果")
                .doWrite(list);
    }
}