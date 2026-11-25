package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 外币期初余额保存 Request VO")
@Data
public class FmsInitialCurrencyBalanceSaveReqVO {

    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Valid
    @NotNull(message = "外币期初不能为空")
    private List<Balance> balances;

    @Data
    public static class Balance {

        @NotNull(message = "科目不能为空")
        private Long subjectId;
        @NotNull(message = "币别不能为空")
        private Long currencyId;
        @DecimalMin(value = "0", message = "期初原币不能小于 0")
        private BigDecimal openingOriginalAmount;
        @DecimalMin(value = "0", message = "期初本位币不能小于 0")
        private BigDecimal openingAmount;
        @DecimalMin(value = "0", message = "本年累计借方原币不能小于 0")
        private BigDecimal yearDebitOriginalAmount;
        @DecimalMin(value = "0", message = "本年累计借方本位币不能小于 0")
        private BigDecimal yearDebitAmount;
        @DecimalMin(value = "0", message = "本年累计贷方原币不能小于 0")
        private BigDecimal yearCreditOriginalAmount;
        @DecimalMin(value = "0", message = "本年累计贷方本位币不能小于 0")
        private BigDecimal yearCreditAmount;
    }

}
