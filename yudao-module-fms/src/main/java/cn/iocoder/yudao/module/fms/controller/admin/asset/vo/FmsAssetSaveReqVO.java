package cn.iocoder.yudao.module.fms.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - FMS 固定资产卡片保存 Request VO")
@Data
public class FmsAssetSaveReqVO {

    @Schema(description = "编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "资产编号；新增时不填则自动生成 ZC0001 这样的编号", example = "ZC0001")
    @Size(max = 32, message = "资产编号不能超过 32 个字")
    private String code;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公电脑")
    @NotBlank(message = "资产名称不能为空")
    @Size(max = 128, message = "资产名称不能超过 128 个字")
    private String name;

    @Schema(description = "资产类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产类别不能为空")
    private Long categoryId;

    @Schema(description = "规格型号")
    @Size(max = 128, message = "规格型号不能超过 128 个字")
    private String spec;

    @Schema(description = "计量单位", example = "台")
    @Size(max = 16, message = "计量单位不能超过 16 个字")
    private String unit;

    @Schema(description = "数量，默认 1", example = "1")
    @DecimalMin(value = "0", inclusive = false, message = "数量必须大于 0")
    private BigDecimal quantity;

    @Schema(description = "购入日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-12-05")
    @NotNull(message = "购入日期不能为空")
    private LocalDate purchaseDate;

    @Schema(description = "启用日期，不填取购入日期；当月增加，下月开始折旧", example = "2026-12-05")
    private LocalDate startDate;

    @Schema(description = "原值", requiredMode = Schema.RequiredMode.REQUIRED, example = "6800.00")
    @NotNull(message = "原值不能为空")
    private BigDecimal originalValue;

    @Schema(description = "净残值率（百分数），不填取类别默认值", example = "5")
    @DecimalMin(value = "0", message = "净残值率不能小于 0")
    @DecimalMax(value = "100", message = "净残值率不能大于 100")
    private BigDecimal residualRate;

    @Schema(description = "折旧月数，不填取类别默认值", example = "36")
    @Min(value = 1, message = "折旧月数必须大于 0")
    private Integer lifeMonths;

    @Schema(description = "进入系统前已折旧月数（旧资产录入用），默认 0", example = "0")
    @Min(value = 0, message = "已折旧月数不能小于 0")
    private Integer initDeprMonths;

    @Schema(description = "进入系统前已提累计折旧（旧资产录入用），默认 0", example = "0.00")
    @DecimalMin(value = "0", message = "累计折旧不能小于 0")
    private BigDecimal initAccumDepr;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注不能超过 255 个字")
    private String remark;

}
