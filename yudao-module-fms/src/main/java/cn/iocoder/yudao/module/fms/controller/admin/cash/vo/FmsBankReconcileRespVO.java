package cn.iocoder.yudao.module.fms.controller.admin.cash.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - FMS 银行余额调节表 Response VO")
@Data
public class FmsBankReconcileRespVO {

    @Schema(description = "日记账余额")
    private BigDecimal bookBalance;

    @Schema(description = "银行对账单余额")
    private BigDecimal bankBalance;

    @Schema(description = "加：企业已收银行未收")
    private BigDecimal bankPlusDepositInTransit;

    @Schema(description = "减：企业已付银行未付")
    private BigDecimal bankLessOutstandingCheck;

    @Schema(description = "加：银行已收企业未收")
    private BigDecimal bookPlusBankCollection;

    @Schema(description = "减：银行已付企业未付")
    private BigDecimal bookLessBankPayment;

    @Schema(description = "调节后余额")
    private BigDecimal adjustedBalance;

    @Schema(description = "两边是否相等")
    private Boolean balanced;

    @Schema(description = "已结账期间：双方都存在但未勾对的项（不计入未达项，单独列示）")
    private java.util.List<FmsUnreconciledSamePeriodItemRespVO> unreconciledSamePeriodItems;

    @Schema(description = "提示信息，例如已结账期间存在两边都有的未勾对项")
    private String warning;

}
