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

@Schema(description = "管理后台 - FMS 固定资产类别保存 Request VO")
@Data
public class FmsAssetCategorySaveReqVO {

    @Schema(description = "编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "类别编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "01")
    @NotBlank(message = "类别编码不能为空")
    @Size(max = 32, message = "类别编码不能超过 32 个字")
    private String code;

    @Schema(description = "类别名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "电子设备")
    @NotBlank(message = "类别名称不能为空")
    @Size(max = 64, message = "类别名称不能超过 64 个字")
    private String name;

    @Schema(description = "默认折旧月数", requiredMode = Schema.RequiredMode.REQUIRED, example = "36")
    @NotNull(message = "默认折旧月数不能为空")
    @Min(value = 1, message = "折旧月数必须大于 0")
    private Integer lifeMonths;

    @Schema(description = "默认净残值率（百分数，5 表示 5%）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "默认净残值率不能为空")
    @DecimalMin(value = "0", message = "净残值率不能小于 0")
    @DecimalMax(value = "100", message = "净残值率不能大于 100")
    private BigDecimal residualRate;

    @Schema(description = "固定资产科目", requiredMode = Schema.RequiredMode.REQUIRED, example = "22")
    @NotNull(message = "固定资产科目不能为空")
    private Long assetSubjectId;

    @Schema(description = "累计折旧科目", requiredMode = Schema.RequiredMode.REQUIRED, example = "23")
    @NotNull(message = "累计折旧科目不能为空")
    private Long accumSubjectId;

    @Schema(description = "折旧费用科目", requiredMode = Schema.RequiredMode.REQUIRED, example = "103")
    @NotNull(message = "折旧费用科目不能为空")
    private Long expenseSubjectId;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注不能超过 255 个字")
    private String remark;

}
