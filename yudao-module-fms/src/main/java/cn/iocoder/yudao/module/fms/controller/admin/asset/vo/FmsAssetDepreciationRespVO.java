package cn.iocoder.yudao.module.fms.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - FMS 固定资产折旧计提 Response VO")
@Data
public class FmsAssetDepreciationRespVO {

    @Schema(description = "会计期间", example = "2027-01")
    private String period;

    @Schema(description = "这个月是否已经计提并生成凭证", example = "false")
    private Boolean generated;

    @Schema(description = "折旧凭证编号（已计提才有）")
    private Long voucherId;

    @Schema(description = "折旧凭证字号，例如 记-14（已计提才有）")
    private String voucherNo;

    @Schema(description = "提醒，例如上个月还没计提")
    private List<String> warnings = new ArrayList<>();

    @Schema(description = "明细：一个资产一行")
    private List<Row> rows = new ArrayList<>();

    @Schema(description = "合计：本月折旧额、原值、累计折旧、净值")
    private Row total;

    @Schema(description = "按类别汇总，凭证里每个类别两行分录")
    private List<CategoryRow> categories = new ArrayList<>();

    @Schema(description = "一个资产本月的折旧")
    @Data
    public static class Row {

        @Schema(description = "资产编号")
        private Long assetId;

        @Schema(description = "资产编号", example = "ZC0001")
        private String code;

        @Schema(description = "资产名称", example = "办公电脑")
        private String name;

        @Schema(description = "类别名称")
        private String categoryName;

        @Schema(description = "原值")
        private BigDecimal originalValue = BigDecimal.ZERO;

        @Schema(description = "本月折旧额")
        private BigDecimal amount = BigDecimal.ZERO;

        @Schema(description = "计提后累计折旧")
        private BigDecimal accumAfter = BigDecimal.ZERO;

        @Schema(description = "计提后净值")
        private BigDecimal netAfter = BigDecimal.ZERO;

        @Schema(description = "已折旧月数（含本月）")
        private Integer depreciatedMonths;

        @Schema(description = "折旧月数")
        private Integer lifeMonths;

    }

    @Schema(description = "一个类别的本月折旧汇总")
    @Data
    public static class CategoryRow {

        @Schema(description = "类别名称")
        private String categoryName;

        @Schema(description = "借：折旧费用科目")
        private String expenseSubjectName;

        @Schema(description = "贷：累计折旧科目")
        private String accumSubjectName;

        @Schema(description = "本月折旧额")
        private BigDecimal amount = BigDecimal.ZERO;

    }

}
