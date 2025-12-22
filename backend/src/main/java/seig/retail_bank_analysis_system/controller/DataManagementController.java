package seig.retail_bank_analysis_system.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.dao.BankTransactionMapper;
import seig.retail_bank_analysis_system.dto.CustomerQueryDTO;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import seig.retail_bank_analysis_system.entity.BankTransaction;
import seig.retail_bank_analysis_system.util.CustomerDataListener;
import seig.retail_bank_analysis_system.util.Log;
import seig.retail_bank_analysis_system.util.Result;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private StringRedisTemplate redisTemplate;

    // ================= 1. 客户基础数据模块 (用于分群分析页) =================

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
    @PostMapping("/import")
    public Result<?> importCustomers(@RequestParam("file") MultipartFile file) throws IOException {
        EasyExcel.read(file.getInputStream(), BankCustomer.class, new CustomerDataListener(customerMapper)).sheet().doRead();

        redisTemplate.delete("dashboard_data_cache");

        return Result.success("客户数据导入成功");
    }


    // ================= 2. 交易流水数据模块 (用于交易分析页) =================

    /**
     * 获取交易流水列表 (展示最近的500条)
     */
    @GetMapping("/transaction-list")
    public Result<?> getTransactionList() {
        return Result.success(transactionMapper.selectList(new LambdaQueryWrapper<BankTransaction>()
                .orderByDesc(BankTransaction::getTransTime)
                .last("LIMIT 100"))); // 仅展示最新100条防止页面卡顿
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

        // 【新增】导入成功后，立即删除首页缓存
        redisTemplate.delete("dashboard_data_cache");

        return Result.success("成功导入 " + list.size() + " 条流水数据");
    }

    // ================= 3. 高级交叉分析模块 (用于多维统计页) =================

    @PostMapping("/search")
    public Result<?> searchCustomers(@RequestBody CustomerQueryDTO query) {
        // 执行多表关联的交叉分析查询（SQL在BankCustomerMapper中定义）
        List<Map<String, Object>> result = customerMapper.crossAnalyze(query);

        // 计算当前筛选结果的综合指标汇总
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPeople", result.size());

        // 计算这批人的平均交易额
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

    @PostMapping("/clear-all")
    public Result<?> clearAllData() {
        // 1. 清空数据库表
        customerMapper.delete(null);
        transactionMapper.delete(null);

        // 2. 【核心】立即清空 Redis 缓存
        redisTemplate.delete("dashboard_data_cache");

        return Result.success("数据库及缓存已全部清空");
    }
}