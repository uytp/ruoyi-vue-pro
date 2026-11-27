package cn.iocoder.yudao.module.fms.controller.admin.settle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 账龄分析 Request VO")
@Data
public class FmsSettleAgingReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "往来科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6")
    @NotNull(message = "往来科目不能为空")
    private Long subjectId;

    @Schema(description = "辅助核算类别编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "辅助核算类别不能为空")
    private Long auxiliaryTypeId;

    @Schema(description = "截止月份 yyyy-MM，账龄按这个月的最后一天计算", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-12")
    @NotNull(message = "截止月份不能为空")
    private String month;

    @Schema(description = "账龄区间天数，逗号隔开，默认 30,60,90,180,365", example = "30,60,90,180,365")
    private String buckets;

}
