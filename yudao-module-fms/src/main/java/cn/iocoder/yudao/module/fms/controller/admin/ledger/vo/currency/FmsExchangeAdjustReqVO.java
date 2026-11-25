package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 期末调汇 Request VO")
@Data
public class FmsExchangeAdjustReqVO {

    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;
    @Schema(description = "汇兑损益科目编号；为空时自动取名称为“汇兑损益”的末级科目")
    private Long profitLossSubjectId;
    @Schema(description = "凭证字编号；为空时取默认凭证字")
    private Long voucherWordId;
    @Schema(description = "期末汇率；未填写的币别取汇率表 / 默认汇率")
    @Valid
    private List<Rate> rates;

    @Data
    public static class Rate {
        @NotNull(message = "币别不能为空")
        private Long currencyId;
        @Digits(integer = 12, fraction = 6, message = "汇率最多保留 6 位小数")
        private BigDecimal rate;
    }

}
