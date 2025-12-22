package seig.retail_bank_analysis_system.util;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CustomerDataListener implements ReadListener<BankCustomer> {

    private final BankCustomerMapper customerMapper;
    // 每隔 100 条存储一次数据库，然后清理列表，防止内存溢出
    private List<BankCustomer> cachedDataList = new ArrayList<>(100);

    public CustomerDataListener(BankCustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Override
    public void invoke(BankCustomer data, AnalysisContext context) {
        cachedDataList.add(data);
        // 达到阈值就存储
        if (cachedDataList.size() >= 100) {
            saveData();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 最后再存储一次剩余的数据
        saveData();
    }

    private void saveData() {
        if (!cachedDataList.isEmpty()) {
            log.info("正在存储 {} 条数据...", cachedDataList.size());
            for (BankCustomer customer : cachedDataList) {
                customer.setId(null); // 确保主键自增，防止冲突
                customerMapper.insert(customer);
            }
            // !!! 重点：存储完后必须立即清空，否则 doAfterAllAnalysed 会再存一遍
            cachedDataList.clear();
        }
    }
}