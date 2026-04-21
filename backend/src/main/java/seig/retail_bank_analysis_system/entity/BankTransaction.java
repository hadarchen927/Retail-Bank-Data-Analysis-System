package seig.retail_bank_analysis_system.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat; // 引入EasyExcel的时间格式化
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

    @ExcelProperty(index = 0) // 强制绑定第 1 列，避开 BOM 乱码问题
    private Long custId;

    @ExcelProperty(index = 1) // 第 2 列
    private BigDecimal amount;

    @ExcelProperty(index = 2) // 第 3 列
    private String type;

    @ExcelProperty(index = 3) // 第 4 列
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss") // 帮 EasyExcel 解析 CSV 中的时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime transTime;

    @ExcelProperty(index = 4) // 第 5 列
    private String location;
}