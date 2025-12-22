package seig.retail_bank_analysis_system.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import seig.retail_bank_analysis_system.entity.RiskRecord;

@Mapper
public interface RiskRecordMapper extends BaseMapper<RiskRecord> {
}