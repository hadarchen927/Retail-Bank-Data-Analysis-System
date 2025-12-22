package seig.retail_bank_analysis_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.*;
import seig.retail_bank_analysis_system.entity.*;
import seig.retail_bank_analysis_system.util.Result;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    @Autowired
    private BankCustomerMapper customerMapper;
    @Autowired
    private BankTransactionMapper transactionMapper;
    @Autowired
    private RiskRecordMapper riskRecordMapper;

    // 核心算法：一键扫描全表风险
    @PostMapping("/scan")
    public Result<?> runRiskScan() {
        // 1. 清空现有的待处理记录（防止重复扫描）
        riskRecordMapper.delete(new LambdaQueryWrapper<RiskRecord>().eq(RiskRecord::getStatus, "待处理"));

        List<BankCustomer> customers = customerMapper.selectList(null);
        int count = 0;

        for (BankCustomer c : customers) {
            // 规则 A：高负债风险判定 (贷款余额 > 资产总额的 80%)
            if (c.getLoanBalance().compareTo(c.getTotalAssets().multiply(new BigDecimal("0.8"))) > 0) {
                saveRisk(c, "高负债风险", "高", "客户贷款规模已逼近其总资产，存在极高违约风险。");
                count++;
            }

            // 规则 B：大额流水风险判定 (是否存在单笔金额 > 5000 的支出)
            List<BankTransaction> bigSpends = transactionMapper.selectList(new LambdaQueryWrapper<BankTransaction>()
                    .eq(BankTransaction::getCustId, c.getId())
                    .eq(BankTransaction::getType, "支出")
                    .gt(BankTransaction::getAmount, new BigDecimal("5000")));

            if (!bigSpends.isEmpty()) {
                saveRisk(c, "异常大额支出", "中", "监测到单笔超过5000元的支出行为，可能存在资金洗钱或财务恶化风险。");
                count++;
            }
        }
        return Result.success("扫描完成，系统自动识别了 " + count + " 条潜在风险点。");
    }

    @GetMapping("/list")
    public Result<?> getRiskList() {
        return Result.success(riskRecordMapper.selectList(new LambdaQueryWrapper<RiskRecord>().orderByDesc(RiskRecord::getCreateTime)));
    }

    @PutMapping("/handle/{id}")
    public Result<?> handleRisk(@PathVariable Long id, @RequestParam String action) {
        RiskRecord record = new RiskRecord();
        record.setId(id);
        record.setStatus(action); // 已处理 或 已忽略
        riskRecordMapper.updateById(record);
        return Result.success("操作成功");
    }

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