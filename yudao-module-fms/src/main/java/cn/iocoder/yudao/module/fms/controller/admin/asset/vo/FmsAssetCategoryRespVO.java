package cn.iocoder.yudao.module.fms.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - FMS 固定资产类别 Response VO")
@Data
public class FmsAssetCategoryRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "类别编码", example = "01")
    private String code;

    @Schema(description = "类别名称", example = "电子设备")
    private String name;

    @Schema(description = "默认折旧月数", example = "36")
    private Integer lifeMonths;

    @Schema(description = "默认净残值率（百分数）", example = "5")
    private BigDecimal residualRate;

    @Schema(description = "固定资产科目编号", example = "22")
    private Long assetSubjectId;

    @Schema(description = "固定资产科目", example = "1601 固定资产")
    private String assetSubjectName;

    @Schema(description = "累计折旧科目编号", example = "23")
    private Long accumSubjectId;

    @Schema(description = "累计折旧科目", example = "1602 累计折旧")
    private String accumSubjectName;

    @Schema(description = "折旧费用科目编号", example = "103")
    private Long expenseSubjectId;

    @Schema(description = "折旧费用科目", example = "560206 折旧费")
    private String expenseSubjectName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "这个类别下有多少张资产卡片", example = "3")
    private Long assetCount;

}
