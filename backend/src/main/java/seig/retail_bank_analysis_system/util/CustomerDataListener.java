package seig.retail_bank_analysis_system.util;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CustomerDataListener implements ReadListener<BankCustomer> {

    private final BankCustomerMapper customerMapper;
    private List<BankCustomer> cachedDataList = new ArrayList<>(100);

    @Getter private int successCount = 0; // 成功录入数
    @Getter private int skipCount = 0;    // 剔除异常数

    public CustomerDataListener(BankCustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Override
    public void invoke(BankCustomer data, AnalysisContext context) {
        // 【核心数据清洗逻辑：不符合规则即剔除】

        // 1. 校验姓名是否缺失
        if (data.getCustName() == null || data.getCustName().trim().isEmpty()) {
            skipCount++;
            return; // 直接返回，不加入 list，实现剔除
        }

        // 2. 校验年龄是否合法
        if (data.getAge() == null || data.getAge() < 18 || data.getAge() > 100) {
            skipCount++;
            return; // 剔除未成年或超龄异常数据
        }

        // 3. 校验金额是否为正数
        if (data.getAnnualIncome() == null || data.getAnnualIncome().compareTo(BigDecimal.ZERO) < 0 ||
                data.getTotalAssets() == null || data.getTotalAssets().compareTo(BigDecimal.ZERO) < 0) {
            skipCount++;
            return; // 剔除负债或收入数据异常行
        }

        // 符合规则，进入缓存队列
        successCount++;
        cachedDataList.add(data);
        if (cachedDataList.size() >= 100) {
            saveData();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    private void saveData() {
        if (!cachedDataList.isEmpty()) {
            for (BankCustomer customer : cachedDataList) {
                customer.setId(null);
                customer.setClusterTag(-1);
                customerMapper.insert(customer);
            }
            cachedDataList.clear();
        }
    }
}