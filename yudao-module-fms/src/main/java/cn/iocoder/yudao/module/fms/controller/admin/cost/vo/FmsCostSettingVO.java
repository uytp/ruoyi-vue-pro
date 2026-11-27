package cn.iocoder.yudao.module.fms.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 成本参数 VO（查询与保存共用）")
@Data
public class FmsCostSettingVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套不能为空")
    private Long accountSetId;

    @Schema(description = "产品对应的辅助核算类别编号（默认存货）", example = "6")
    @NotNull(message = "请选择产品对应的辅助核算类别")
    private Long productAuxTypeId;

    @Schema(description = "生产成本-直接材料 科目编号")
    @NotNull(message = "请选择直接材料科目")
    private Long materialSubjectId;

    @Schema(description = "生产成本-直接人工 科目编号")
    @NotNull(message = "请选择直接人工科目")
    private Long laborSubjectId;

    @Schema(description = "生产成本-制造费用 科目编号（分配转入）")
    @NotNull(message = "请选择生产成本-制造费用科目")
    private Long overheadInSubjectId;

    @Schema(description = "制造费用归集科目编号")
    @NotNull(message = "请选择制造费用归集科目")
    private Long overheadPoolSubjectId;

    @Schema(description = "库存商品科目编号")
    @NotNull(message = "请选择库存商品科目")
    private Long finishedSubjectId;

    @Schema(description = "主营业务成本科目编号")
    @NotNull(message = "请选择主营业务成本科目")
    private Long cogsSubjectId;

    @Schema(description = "制造费用分配方法：1完工数量 2生产工时 3直接人工 4直接成本", example = "1")
    @NotNull(message = "请选择制造费用分配方法")
    @Min(value = 1, message = "分配方法不正确")
    @Max(value = 4, message = "分配方法不正确")
    private Integer overheadAllocMethod;

    @Schema(description = "在产品：0不计算在产品 1约当产量法", example = "0")
    @NotNull(message = "请选择在产品处理方式")
    @Min(value = 0, message = "在产品处理方式不正确")
    @Max(value = 1, message = "在产品处理方式不正确")
    private Integer wipMethod;

    @Schema(description = "材料投入方式：1开工一次投入 2随加工进度投入", example = "1")
    @NotNull(message = "请选择材料投入方式")
    @Min(value = 1, message = "材料投入方式不正确")
    @Max(value = 2, message = "材料投入方式不正确")
    private Integer materialInputMode;

}
