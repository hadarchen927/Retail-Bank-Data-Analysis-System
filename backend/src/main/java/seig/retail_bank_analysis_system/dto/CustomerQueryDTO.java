package seig.retail_bank_analysis_system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerQueryDTO {
    // 原有客户维度
    private String custName;
    private String gender;
    private Integer ageMin;
    private Integer ageMax;
    private Integer clusterTag;

    // 新增交易行为维度
    private Integer minTransCount;      // 最少交易次数
    private BigDecimal minTotalAmount;  // 累计交易金额总计
    private String preferredTransType;  // 偏好的交易类型 (存入/支出/转账)
}

