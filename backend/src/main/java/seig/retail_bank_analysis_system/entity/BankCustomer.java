package seig.retail_bank_analysis_system.entity;

import com.alibaba.excel.annotation.ExcelIgnore; // 导入这个
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("bank_customer")
public class BankCustomer {
    @TableId(type = IdType.AUTO)
    @ExcelProperty("id") // 只有导出时有用
    private Long id;

    @ExcelProperty("custName") // 必须对应 Python 里的 custName
    private String custName;

    @ExcelProperty("gender") // 必须对应 Python 里的 gender
    private String gender;

    @ExcelProperty("age")
    private Integer age;

    @ExcelProperty("annualIncome")
    private java.math.BigDecimal annualIncome;

    @ExcelProperty("totalAssets")
    private java.math.BigDecimal totalAssets;

    @ExcelProperty("loanBalance")
    private java.math.BigDecimal loanBalance;

    private Integer clusterTag;
}