package seig.retail_bank_analysis_system.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import seig.retail_bank_analysis_system.entity.BankTransaction;

import java.util.List;
import java.util.Map;

@Mapper
public interface BankTransactionMapper extends BaseMapper<BankTransaction> {

    // 1. 原有的：按类型统计（用于饼图）
    @Select("SELECT type as name, COUNT(*) as value FROM bank_transaction GROUP BY type")
    List<Map<String, Object>> getTransactionTypeStats();

    // 2. 新增：趋势统计（用于仪表盘折线图）
    // DATE_FORMAT 是 MySQL 函数，用于提取年月，展示近 6 个月趋势
    @Select("SELECT DATE_FORMAT(trans_time, '%Y-%m') as month, SUM(amount) as total " +
            "FROM bank_transaction " +
            "GROUP BY month " +
            "ORDER BY month ASC " +
            "LIMIT 6")
    List<Map<String, Object>> getMonthlyTrend();

    @Select("SELECT type as name, SUM(amount) as value " +
            "FROM bank_transaction " +
            "GROUP BY type " +
            "ORDER BY value DESC")
    List<Map<String, Object>> getProductSalesStats();
}