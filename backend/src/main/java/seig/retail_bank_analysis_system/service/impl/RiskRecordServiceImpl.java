package seig.retail_bank_analysis_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import seig.retail_bank_analysis_system.dao.RiskRecordMapper;
import seig.retail_bank_analysis_system.entity.RiskRecord;
import seig.retail_bank_analysis_system.service.RiskRecordService;

/**
 * 风险预警记录 服务层实现类
 */
@Service
public class RiskRecordServiceImpl extends ServiceImpl<RiskRecordMapper, RiskRecord> implements RiskRecordService {

}