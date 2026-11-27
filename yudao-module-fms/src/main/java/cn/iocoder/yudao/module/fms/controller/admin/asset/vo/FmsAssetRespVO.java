package cn.iocoder.yudao.module.fms.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - FMS 固定资产卡片 Response VO")
@Data
public class FmsAssetRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "资产编号", example = "ZC0001")
    private String code;

    @Schema(description = "资产名称", example = "办公电脑")
    private String name;

    @Schema(description = "资产类别编号", example = "1")
    private Long categoryId;

    @Schema(description = "资产类别名称", example = "电子设备")
    private String categoryName;

    @Schema(description = "规格型号")
    private String spec;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "购入日期")
    private LocalDate purchaseDate;

    @Schema(description = "启用日期")
    private LocalDate startDate;

    @Schema(description = "原值", example = "6800.00")
    private BigDecimal originalValue;

    @Schema(description = "净残值率（百分数）", example = "5")
    private BigDecimal residualRate;

    @Schema(description = "净残值", example = "340.00")
    private BigDecimal residualValue;

    @Schema(description = "折旧月数", example = "36")
    private Integer lifeMonths;

    @Schema(description = "进入系统前已折旧月数")
    private Integer initDeprMonths;

    @Schema(description = "进入系统前已提累计折旧")
    private BigDecimal initAccumDepr;

    @Schema(description = "已折旧月数（含进入系统前的）")
    private Integer depreciatedMonths;

    @Schema(description = "剩余折旧月数")
    private Integer remainMonths;

    @Schema(description = "累计折旧（含进入系统前的）", example = "179.56")
    private BigDecimal accumDepr;

    @Schema(description = "净值 = 原值 - 累计折旧", example = "6620.44")
    private BigDecimal netValue;

    @Schema(description = "按公式算的月折旧额", example = "179.56")
    private BigDecimal monthlyDepr;

    @Schema(description = "第一次计提折旧的月份 yyyy-MM；在用的资产才有", example = "2027-01")
    private String firstDeprMonth;

    @Schema(description = "应该折旧但因为已经结账、没有计提的月数（只是提醒，需要补的话用“进入系统前已折旧”录入）", example = "1")
    private Integer missedMonths;

    @Schema(description = "状态：0 在用 1 已处置", example = "0")
    private Integer status;

    @Schema(description = "处置日期")
    private LocalDate disposeDate;

    @Schema(description = "处置（转入清理）凭证编号")
    private Long disposeVoucherId;

    @Schema(description = "是否已经计提过折旧（计提过就不能再改原值等关键信息）", example = "false")
    private Boolean hasDepreciation;

    @Schema(description = "备注")
    private String remark;

}
