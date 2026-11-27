package cn.iocoder.yudao.module.fms.controller.admin.settle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 往来未核销项目查询 Request VO")
@Data
public class FmsSettleItemQueryReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "往来科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6")
    @NotNull(message = "往来科目不能为空")
    private Long subjectId;

    @Schema(description = "辅助核算类别编号（客户 / 供应商 / 职员）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "辅助核算类别不能为空")
    private Long auxiliaryTypeId;

    @Schema(description = "辅助核算项目编号（具体的客户 / 供应商 / 职员）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "往来单位不能为空")
    private Long auxiliaryItemId;

}
