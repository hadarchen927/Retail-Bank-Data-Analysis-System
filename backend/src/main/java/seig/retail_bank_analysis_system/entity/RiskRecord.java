package seig.retail_bank_analysis_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("bank_risk_record")
public class RiskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long custId;
    private String custName;
    private String riskType;
    private String riskLevel;
    private String description;
    private String status; // 待处理, 已处理

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}