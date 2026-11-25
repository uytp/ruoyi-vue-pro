package cn.iocoder.yudao.module.fms.controller.admin.cash.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - FMS 银行对账单 Response VO")
@Data
public class FmsBankStatementRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "交易日期")
    private LocalDate statementDate;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "收入金额")
    private BigDecimal debitAmount;

    @Schema(description = "支出金额")
    private BigDecimal creditAmount;

    @Schema(description = "余额")
    private BigDecimal balance;

    @Schema(description = "流水号")
    private String referenceNo;

    @Schema(description = "是否已勾对")
    private Boolean reconciled;

    @Schema(description = "勾对的凭证分录编号")
    private Long voucherEntryId;

}
