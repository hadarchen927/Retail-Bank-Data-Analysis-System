package seig.retail_bank_analysis_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.*;
import seig.retail_bank_analysis_system.entity.*;
import seig.retail_bank_analysis_system.util.Result;
import seig.retail_bank_analysis_system.util.Log;

import java.math.BigDecimal;
import java.util.List;

/**
 * 风险预警监控控制器
 * 对应任务书模块 7：风险预警模块
 */
@RestController
@RequestMapping("/api/risk")
public class RiskController {

    @Autowired
    private BankCustomerMapper customerMapper;
    @Autowired
    private BankTransactionMapper transactionMapper;
    @Autowired
    private RiskRecordMapper riskRecordMapper;

    /**
     * 执行一键风险扫描
     * 逻辑：扫描所有客户，根据负债比和异常流水判定风险
     */
    @Log("执行全表风险扫描")
    @PostMapping("/scan")
    public Result<?> runRiskScan() {
        try {
            // 1. 获取所有客户，若为空则直接返回
            List<BankCustomer> customers = customerMapper.selectList(null);
            if (customers == null || customers.isEmpty()) {
                return Result.error("客户数据为空，请先导入客户表再进行扫描。");
            }

            // 2. 清空现有的“待处理”记录，避免重复累积
            riskRecordMapper.delete(null);

            int count = 0;
            for (BankCustomer c : customers) {
                // --- 规则数据准备（防止空指针） ---
                BigDecimal assets = c.getTotalAssets() != null ? c.getTotalAssets() : BigDecimal.ZERO;
                BigDecimal loan = c.getLoanBalance() != null ? c.getLoanBalance() : BigDecimal.ZERO;

                // --- 规则 A：高负债风险判定 (贷款 > 资产 * 0.8) ---
                // 只有当资产大于0时才计算比例
                if (assets.compareTo(BigDecimal.ZERO) > 0) {
                    if (loan.compareTo(assets.multiply(new BigDecimal("0.1"))) > 0) {
                        saveRisk(c, "高负债风险", "高", "客户贷款余额已达资产 10% 以上。");
                        count++;
                        continue;
                    }
                }

                // --- 规则 B：大额支出预警 (单笔支出 > 5000) ---
                List<BankTransaction> bigSpends = transactionMapper.selectList(new LambdaQueryWrapper<BankTransaction>()
                        .eq(BankTransaction::getCustId, c.getId())
                        .eq(BankTransaction::getType, "支出")
                        .gt(BankTransaction::getAmount, new BigDecimal("500")));

                if (!bigSpends.isEmpty()) {
                    saveRisk(c, "异常大额支出", "中", "监测到近期存在单笔超过5000元的支出流水。");
                    count++;
                }
            }

            return Result.success("风险扫描完成！系统已自动标记 " + count + " 条潜在风险点。");
        } catch (Exception e) {
            e.printStackTrace(); // 在 IDEA 控制台打印错误堆栈
            return Result.error("系统扫描异常：" + e.getMessage());
        }
    }

    /**
     * 获取风险预警列表
     */
    @GetMapping("/list")
    public Result<?> getRiskList() {
        // 按时间倒序返回所有预警
        return Result.success(riskRecordMapper.selectList(
                new LambdaQueryWrapper<RiskRecord>().orderByDesc(RiskRecord::getCreateTime)
        ));
    }

    /**
     * 处理风险预警（标记或忽略）
     */
    @Log("决策处理风险预警")
    @PutMapping("/handle/{id}")
    public Result<?> handleRisk(@PathVariable Long id, @RequestParam String action) {
        RiskRecord record = new RiskRecord();
        record.setId(id);
        record.setStatus(action); // 修改为：已标记 / 已忽略
        riskRecordMapper.updateById(record);
        return Result.success("预警处理状态已更新为：" + action);
    }

    /**
     * 内部辅助方法：保存预警记录
     */
    private void saveRisk(BankCustomer c, String type, String level, String desc) {
        RiskRecord r = new RiskRecord();
        r.setCustId(c.getId());
        r.setCustName(c.getCustName());
        r.setRiskType(type);
        r.setRiskLevel(level);
        r.setDescription(desc);
        r.setStatus("待处理");
        riskRecordMapper.insert(r);
    }
}