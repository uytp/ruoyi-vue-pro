package cn.iocoder.yudao.module.fms.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - FMS 增值税申报汇总 Response VO")
@Data
public class FmsTaxVatRespVO {

    @Schema(description = "税款所属期", example = "2027-01")
    private String period;

    @Schema(description = "销项：按税率分组")
    private List<RateRow> salesRows = new ArrayList<>();

    @Schema(description = "销售额合计（不含税）")
    private BigDecimal salesAmount = BigDecimal.ZERO;

    @Schema(description = "销项税额合计")
    private BigDecimal outputTax = BigDecimal.ZERO;

    @Schema(description = "进项：本月认证抵扣的专票，按税率分组")
    private List<RateRow> inputRows = new ArrayList<>();

    @Schema(description = "本月可抵扣的进项税额合计")
    private BigDecimal inputTax = BigDecimal.ZERO;

    @Schema(description = "已收到但还没认证的进项专票张数")
    private Integer uncertifiedCount = 0;

    @Schema(description = "已收到但还没认证的进项专票税额（认证后才能抵扣）")
    private BigDecimal uncertifiedTax = BigDecimal.ZERO;

    @Schema(description = "上期留抵税额")
    private BigDecimal priorCarry = BigDecimal.ZERO;

    @Schema(description = "抵扣后的净额：正数要交税，负数是留抵")
    private BigDecimal net = BigDecimal.ZERO;

    @Schema(description = "本月应纳增值税")
    private BigDecimal payable = BigDecimal.ZERO;

    @Schema(description = "期末留抵税额（带到下个月）")
    private BigDecimal carryEnd = BigDecimal.ZERO;

    @Schema(description = "附加税明细")
    private List<SurchargeRow> surcharges = new ArrayList<>();

    @Schema(description = "附加税合计")
    private BigDecimal surchargeTotal = BigDecimal.ZERO;

    @Schema(description = "本月合计应交税费 = 应纳增值税 + 附加税")
    private BigDecimal totalPayable = BigDecimal.ZERO;

    @Schema(description = "是否已经生成税务凭证", example = "false")
    private Boolean generated = false;

    @Schema(description = "税务凭证编号")
    private Long voucherId;

    @Schema(description = "税务凭证字号，例如 记-18")
    private String voucherNo;

    @Schema(description = "提醒")
    private List<String> warnings = new ArrayList<>();

    @Schema(description = "与总账对数：发票汇总的税额 vs 总账科目的本期发生额")
    private List<ReconcileRow> reconcile = new ArrayList<>();

    @Schema(description = "按税率分组的一行")
    @Data
    public static class RateRow {

        @Schema(description = "税率（百分数）", example = "13")
        private BigDecimal taxRate;

        @Schema(description = "发票张数")
        private Integer count = 0;

        @Schema(description = "不含税金额")
        private BigDecimal amount = BigDecimal.ZERO;

        @Schema(description = "税额")
        private BigDecimal taxAmount = BigDecimal.ZERO;

    }

    @Schema(description = "一种附加税")
    @Data
    public static class SurchargeRow {

        @Schema(description = "名称", example = "城市维护建设税")
        private String name;

        @Schema(description = "税率（百分数）")
        private BigDecimal rate;

        @Schema(description = "计税依据（本月应纳增值税）")
        private BigDecimal base = BigDecimal.ZERO;

        @Schema(description = "金额")
        private BigDecimal amount = BigDecimal.ZERO;

        @Schema(description = "借：税金及附加 科目")
        private String expenseSubjectName;

        @Schema(description = "贷：应交税费 科目")
        private String payableSubjectName;

    }

    @Schema(description = "一行对数")
    @Data
    public static class ReconcileRow {

        @Schema(description = "名称", example = "销项税额")
        private String name;

        @Schema(description = "总账科目", example = "22210107 销项税额")
        private String subjectName;

        @Schema(description = "发票汇总")
        private BigDecimal invoiceAmount = BigDecimal.ZERO;

        @Schema(description = "总账本期发生额")
        private BigDecimal ledgerAmount = BigDecimal.ZERO;

        @Schema(description = "差额 = 发票 - 总账")
        private BigDecimal diff = BigDecimal.ZERO;

    }

}
