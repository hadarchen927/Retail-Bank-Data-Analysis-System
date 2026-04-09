package seig.retail_bank_analysis_system.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import seig.retail_bank_analysis_system.entity.BankTransaction;

import java.util.List;
import java.util.Map;

@Mapper
public interface BankTransactionMapper extends BaseMapper<BankTransaction> {

    @Select("SELECT type as name, COUNT(*) as value FROM bank_transaction GROUP BY type")
    List<Map<String, Object>> getTransactionTypeStats();

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