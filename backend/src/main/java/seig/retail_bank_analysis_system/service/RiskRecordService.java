package seig.retail_bank_analysis_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import seig.retail_bank_analysis_system.entity.RiskRecord;

/**
 * 风险预警记录 服务层接口
 * 继承 MyBatis-Plus 的 IService，获得 saveBatch 等高级批量操作能力
 */
public interface RiskRecordService extends IService<RiskRecord> {

}