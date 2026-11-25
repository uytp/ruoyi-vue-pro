package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 期末调汇预览 Response VO")
@Data
public class FmsExchangeAdjustRespVO {

    @Schema(description = "调汇会计期间")
    private String month;
    private Long profitLossSubjectId;
    private String profitLossSubjectCode;
    private String profitLossSubjectName;
    private List<FmsCurrencyRateRespVO> rates;
    private List<Line> lines;
    @Schema(description = "汇兑差额合计（正数 = 借记外币科目 / 汇兑收益）")
    private BigDecimal totalDifference;
    @Schema(description = "生成的凭证编号")
    private Long voucherId;

    @Data
    public static class Line {
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private Long currencyId;
        private String currencyCode;
        private String currencyName;
        private BigDecimal rate;
        @Schema(description = "期末余额方向")
        private String direction;
        @Schema(description = "期末原币余额（按方向取正）")
        private BigDecimal endingOriginalAmount;
        @Schema(description = "调汇前本位币余额（按方向取正）")
        private BigDecimal endingAmount;
        @Schema(description = "调汇后本位币余额（按方向取正）")
        private BigDecimal adjustedAmount;
        @Schema(description = "汇兑差额：正数借记该科目，负数贷记该科目")
        private BigDecimal differenceAmount;
    }

}
