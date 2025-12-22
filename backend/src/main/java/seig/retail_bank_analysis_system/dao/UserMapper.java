package seig.retail_bank_analysis_system.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import seig.retail_bank_analysis_system.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}