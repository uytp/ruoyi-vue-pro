package cn.iocoder.yudao.module.fms.controller.admin.settle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - FMS 往来核销 Request VO")
@Data
public class FmsSettleCreateReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "往来科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6")
    @NotNull(message = "往来科目不能为空")
    private Long subjectId;

    @Schema(description = "辅助核算类别编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "辅助核算类别不能为空")
    private Long auxiliaryTypeId;

    @Schema(description = "辅助核算项目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "往来单位不能为空")
    private Long auxiliaryItemId;

    @Schema(description = "核销日期，不传则为今天")
    private LocalDate settleDate;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注不能超过 255 个字")
    private String remark;

    @Schema(description = "本次核销的借方项目", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择要核销的借方项目")
    @Valid
    private List<ItemAmount> debitItems;

    @Schema(description = "本次核销的贷方项目", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择要核销的贷方项目")
    @Valid
    private List<ItemAmount> creditItems;

    @Schema(description = "一笔核销项目和本次核销金额")
    @Data
    public static class ItemAmount {

        @Schema(description = "来源标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "E123")
        @NotNull(message = "来源标识不能为空")
        private String ref;

        @Schema(description = "本次核销金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
        @NotNull(message = "核销金额不能为空")
        private BigDecimal amount;

    }

}
