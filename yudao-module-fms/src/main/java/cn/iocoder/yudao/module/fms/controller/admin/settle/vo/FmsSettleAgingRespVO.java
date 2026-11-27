package cn.iocoder.yudao.module.fms.controller.admin.settle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - FMS 账龄分析 Response VO")
@Data
public class FmsSettleAgingRespVO {

    @Schema(description = "账龄计算的截止日期（所选月份的最后一天）")
    private LocalDate asOfDate;

    @Schema(description = "余额方向：1 借（应收类） 2 贷（应付类）", example = "1")
    private Integer balanceDirection;

    @Schema(description = "账龄区间名称，例如 0-30天、31-60天 …")
    private List<String> bucketLabels = new ArrayList<>();

    @Schema(description = "每个往来单位一行")
    private List<Row> rows = new ArrayList<>();

    @Schema(description = "合计行")
    private Row total;

    @Schema(description = "一个往来单位的账龄")
    @Data
    public static class Row {

        @Schema(description = "辅助核算项目编号；合计行为空")
        private Long auxiliaryItemId;

        @Schema(description = "编码", example = "C001")
        private String code;

        @Schema(description = "名称", example = "客户甲")
        private String name;

        @Schema(description = "正方向未核销合计（应收账款是借方未核销，应付账款是贷方未核销）", example = "3000.00")
        private BigDecimal mainTotal = BigDecimal.ZERO;

        @Schema(description = "反方向未核销合计（收款或付款还没有对上单据）", example = "500.00")
        private BigDecimal oppositeTotal = BigDecimal.ZERO;

        @Schema(description = "余额 = 正方向 - 反方向，应当等于账上这个单位的科目余额", example = "2500.00")
        private BigDecimal netBalance = BigDecimal.ZERO;

        @Schema(description = "正方向未核销金额按账龄区间分布，顺序和 bucketLabels 一致")
        private List<BigDecimal> buckets = new ArrayList<>();

    }

}
