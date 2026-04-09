package seig.retail_bank_analysis_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.*;
import seig.retail_bank_analysis_system.entity.*;
import seig.retail_bank_analysis_system.service.RiskRecordService;
import seig.retail_bank_analysis_system.util.Result;
import seig.retail_bank_analysis_system.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 风险预警监控控制器
 * 对应任务书模块 7：风险预警模块
 */
@RestController
@RequestMapping("/api/risk") // 确保这里的路径写对了，解决 404 的关键
public class RiskController {

    @Autowired
    private BankCustomerMapper customerMapper;

    @Autowired
    private BankTransactionMapper transactionMapper;

    @Autowired
    private RiskRecordMapper riskRecordMapper;

    // 引入刚刚新建的 Service 层，用于极速批量插入
    @Autowired
    private RiskRecordService riskRecordService;

    /**
     * 执行一键风险扫描
     * 逻辑：扫描所有客户，根据负债比、异常流水、逾期、信用分判定风险
     */
    @Log("执行全表风险扫描")
    @PostMapping("/scan")
    public Result<?> runRiskScan() {
        try {
            // 1. 获取所有客户数据
            List<BankCustomer> customers = customerMapper.selectList(null);
            if (customers == null || customers.isEmpty()) {
                return Result.error("客户数据为空，请先导入 bank_customers.csv");
            }

            // 2. 清空旧的“待处理”预警，保留人工已处理的历史台账
            riskRecordMapper.delete(new LambdaQueryWrapper<RiskRecord>()
                    .eq(RiskRecord::getStatus, "待处理"));

            // 3. 【性能优化】一次性查出所有大于 5000 的大额支出流水，并在内存中按客户ID分组
            List<BankTransaction> allBigSpends = transactionMapper.selectList(
                    new LambdaQueryWrapper<BankTransaction>()
                            .eq(BankTransaction::getType, "支出")
                            .gt(BankTransaction::getAmount, new BigDecimal("5000"))
            );

            Map<Long, List<BankTransaction>> bigSpendMap = allBigSpends.stream()
                    .collect(Collectors.groupingBy(BankTransaction::getCustId));

            int count = 0;
            List<RiskRecord> newRisks = new ArrayList<>();

            // 4. 遍历客户，执行内存级极速风控规则匹配
            for (BankCustomer c : customers) {
                BigDecimal assets = c.getTotalAssets() != null ? c.getTotalAssets() : BigDecimal.ZERO;
                BigDecimal loan = c.getLoanBalance() != null ? c.getLoanBalance() : BigDecimal.ZERO;

                // 规则 A：高负债风险 (贷款 > 资产 * 10%)
                if (assets.compareTo(BigDecimal.ZERO) > 0 && loan.compareTo(assets.multiply(new BigDecimal("0.1"))) > 0) {
                    newRisks.add(createRiskRecord(c, "高负债风险", "高", "客户贷款余额已达资产 10% 以上。"));
                    count++;
                }

                // 规则 B：异常大额支出 (直接查内存 Map，速度极快)
                if (bigSpendMap.containsKey(c.getId())) {
                    newRisks.add(createRiskRecord(c, "异常大额支出", "中", "监测到近期存在单笔超过 5000 元的支出流水。"));
                    count++;
                }

                // 规则 C：信贷逾期风险
                if (c.getOverdueDays() != null && c.getOverdueDays() > 0) {
                    newRisks.add(createRiskRecord(c, "信贷逾期风险", "高", "该客户当前存在 " + c.getOverdueDays() + " 天的逾期记录，需立即催收。"));
                    count++;
                }

                // 规则 D：信用评分极低
                if (c.getCreditScore() != null && c.getCreditScore() < 500) {
                    newRisks.add(createRiskRecord(c, "信用评分极低", "中", "客户信用评分仅为 " + c.getCreditScore() + "，建议限制其新增授信。"));
                    count++;
                }
            }

            // 5. 【性能优化】使用 Service 进行批量插入，消除 N+1 数据库IO操作
            if (!newRisks.isEmpty()) {
                riskRecordService.saveBatch(newRisks);
            }

            return Result.success("风险扫描极速完成！系统已自动标记 " + count + " 条潜在风险点。");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("系统扫描异常：" + e.getMessage());
        }
    }

    /**
     * 获取风险预警列表 (解决前端 404 No static resource 的核心接口)
     */
    @GetMapping("/list")
    public Result<?> getRiskList() {
        // 按创建时间倒序返回所有预警记录
        List<RiskRecord> records = riskRecordMapper.selectList(
                new LambdaQueryWrapper<RiskRecord>().orderByDesc(RiskRecord::getCreateTime)
        );
        return Result.success(records);
    }

    /**
     * 处理风险预警（状态标记为：已处理 / 已忽略）
     */
    @Log("决策处理风险预警")
    @PutMapping("/handle/{id}")
    public Result<?> handleRisk(@PathVariable Long id, @RequestParam String action) {
        RiskRecord record = new RiskRecord();
        record.setId(id);
        record.setStatus(action);
        riskRecordMapper.updateById(record);
        return Result.success("预警处理状态已更新为：" + action);
    }

    /**
     * 内部辅助方法：组装风险记录对象
     */
    private RiskRecord createRiskRecord(BankCustomer c, String type, String level, String desc) {
        RiskRecord r = new RiskRecord();
        r.setCustId(c.getId());
        r.setCustName(c.getCustName());
        r.setRiskType(type);
        r.setRiskLevel(level);
        r.setDescription(desc);
        r.setStatus("待处理");
        return r;
    }
}