package cn.iocoder.yudao.module.fms.controller.admin.settle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - FMS 往来核销记录（按批次）Response VO")
@Data
public class FmsSettleBatchRespVO {

    @Schema(description = "核销批次号，撤销时使用", example = "12")
    private Long batchId;

    @Schema(description = "核销日期")
    private LocalDate settleDate;

    @Schema(description = "本批核销金额", example = "3000.00")
    private BigDecimal amount;

    @Schema(description = "借方来源说明，例如：记-3 赊销商品给客户甲 3,000.00")
    private String debitDesc;

    @Schema(description = "贷方来源说明")
    private String creditDesc;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "操作人")
    private String creator;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;

}
