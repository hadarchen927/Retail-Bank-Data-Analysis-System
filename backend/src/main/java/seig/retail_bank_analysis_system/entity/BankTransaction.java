package seig.retail_bank_analysis_system.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bank_transaction")
public class BankTransaction {
    @TableId(type = IdType.AUTO)
    @ExcelIgnore // 导入时忽略 ID 列
    private Long id;

    @ExcelProperty("custId") // 必须对应 Python 生成的 Excel 表头
    private Long custId;

    @ExcelProperty("amount")
    private BigDecimal amount;

    @ExcelProperty("type")
    private String type;

    @ExcelProperty("transTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime transTime;

    @ExcelProperty("location")
    private String location;
}