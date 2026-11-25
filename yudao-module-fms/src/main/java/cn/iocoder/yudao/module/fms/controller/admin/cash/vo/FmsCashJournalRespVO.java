package cn.iocoder.yudao.module.fms.controller.admin.cash.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - FMS 出纳日记账分录 Response VO")
@Data
public class FmsCashJournalRespVO {

    @Schema(description = "凭证分录编号")
    private Long entryId;

    @Schema(description = "凭证编号")
    private Long voucherId;

    @Schema(description = "凭证日期")
    private LocalDateTime voucherTime;

    @Schema(description = "凭证字号")
    private String voucherNo;

    @Schema(description = "摘要")
    private String digest;

    @Schema(description = "对方科目概述")
    private String counterpartSubject;

    @Schema(description = "借方金额")
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额")
    private BigDecimal creditAmount;

    @Schema(description = "余额")
    private BigDecimal balance;

    @Schema(description = "出纳已勾对")
    private Boolean cashReconciled;

}
