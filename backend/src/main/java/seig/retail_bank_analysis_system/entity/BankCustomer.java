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
    @ExcelProperty("id") // 修改这里：允许 EasyExcel 读写 ID
    private Long id;

    @ExcelProperty("custName")
    private String custName;

    @ExcelProperty("gender")
    private String gender;

    @ExcelProperty("age")
    private Integer age;

    @ExcelProperty("annualIncome")
    private BigDecimal annualIncome;

    @ExcelProperty("totalAssets")
    private BigDecimal totalAssets;

    @ExcelProperty("loanBalance")
    private BigDecimal loanBalance;

    @ExcelProperty("clusterTag") // 修改这里：确保能读到 Python 生成的标签
    private Integer clusterTag;
}