package seig.retail_bank_analysis_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import seig.retail_bank_analysis_system.dao.BankCustomerMapper;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import seig.retail_bank_analysis_system.service.BankCustomerService;

/**
 * 客户数据服务层实现类
 * 继承 ServiceImpl，MyBatis-Plus 会自动将 Mapper 注入并完成批量操作的底层逻辑
 */
@Service
public class BankCustomerServiceImpl extends ServiceImpl<BankCustomerMapper, BankCustomer> implements BankCustomerService {

}