package cn.iocoder.yudao.module.fms.controller.admin.settle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - FMS 往来未核销项目 Response VO")
@Data
public class FmsSettleOpenItemRespVO {

    @Schema(description = "来源标识：E+分录编号，或 O+科目编号-辅助组合编号（期初余额）", example = "E123")
    private String ref;

    @Schema(description = "是否期初余额", example = "false")
    private Boolean opening;

    @Schema(description = "凭证编号（期初余额为空）", example = "45")
    private Long voucherId;

    @Schema(description = "凭证字号，例如 记-3（期初余额为空）", example = "记-3")
    private String voucherNo;

    @Schema(description = "凭证日期；期初余额为账套启用日期")
    private LocalDate voucherDate;

    @Schema(description = "摘要", example = "赊销商品给客户甲")
    private String digest;

    @Schema(description = "方向：1 借 2 贷", example = "1")
    private Integer direction;

    @Schema(description = "原始金额", example = "3000.00")
    private BigDecimal amount;

    @Schema(description = "已核销金额", example = "0.00")
    private BigDecimal settledAmount;

    @Schema(description = "未核销余额", example = "3000.00")
    private BigDecimal remainAmount;

    @Schema(description = "账龄天数（截止日期减去凭证日期）", example = "35")
    private Integer ageDays;

}
