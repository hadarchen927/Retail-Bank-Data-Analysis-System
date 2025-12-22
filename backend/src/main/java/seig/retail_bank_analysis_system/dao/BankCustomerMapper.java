package seig.retail_bank_analysis_system.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import seig.retail_bank_analysis_system.dto.CustomerQueryDTO;
import seig.retail_bank_analysis_system.entity.BankCustomer;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BankCustomerMapper extends BaseMapper<BankCustomer> {

    @Select("SELECT COUNT(*) as totalCustomers, " +
            "IFNULL(SUM(total_assets), 0) as totalAssets, " +
            "IFNULL(SUM(loan_balance), 0) as totalLoans " +
            "FROM bank_customer")
    Map<String, Object> getGlobalStats();

    @Select("SELECT gender as name, COUNT(*) as value FROM bank_customer GROUP BY gender")
    List<Map<String, Object>> getGenderStats();

    @Select("<script>" +
            "SELECT c.*, COUNT(t.id) as trans_count, SUM(t.amount) as total_trans_amount " +
            "FROM bank_customer c " +
            "LEFT JOIN bank_transaction t ON c.id = t.cust_id " +
            "WHERE 1=1 " +
            "<if test='q.custName != null'> AND c.cust_name LIKE CONCAT('%',#{q.custName},'%') </if>" +
            "<if test='q.gender != null'> AND c.gender = #{q.gender} </if>" +
            "<if test='q.ageMin != null'> AND c.age >= #{q.ageMin} </if>" +
            "<if test='q.ageMax != null'> AND c.age &lt;= #{q.ageMax} </if>" +
            "<if test='q.clusterTag != null'> AND c.cluster_tag = #{q.clusterTag} </if>" +
            "GROUP BY c.id " +
            "HAVING 1=1 " +
            "<if test='q.minTransCount != null'> AND trans_count >= #{q.minTransCount} </if>" +
            "<if test='q.minTotalAmount != null'> AND total_trans_amount >= #{q.minTotalAmount} </if>" +
            "</script>")
    List<Map<String, Object>> crossAnalyze(@Param("q") CustomerQueryDTO query);
}