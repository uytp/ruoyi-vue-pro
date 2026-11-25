package cn.iocoder.yudao.module.fms.controller.admin.cash.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - FMS 已结账期间双方都有但未勾对的项")
@Data
public class FmsUnreconciledSamePeriodItemRespVO {

    @Schema(description = "日记账分录编号")
    private Long entryId;

    @Schema(description = "对账单编号")
    private Long statementId;

    @Schema(description = "日记账日期")
    private LocalDate bookDate;

    @Schema(description = "对账单日期")
    private LocalDate statementDate;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "金额（收入为正、支出为负）")
    private BigDecimal amount;
}
