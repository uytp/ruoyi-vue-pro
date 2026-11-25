package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 外币期初余额 Response VO")
@Data
public class FmsInitialCurrencyBalanceRespVO {

    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    @Schema(description = "科目余额方向：1 借 2 贷")
    private Integer balanceDirection;
    @Schema(description = "科目是否启用辅助核算（启用时不自动同步到科目期初）")
    private Boolean auxiliaryAccounting;
    private Long currencyId;
    private String currencyCode;
    private String currencyName;
    @Schema(description = "币别默认汇率")
    private BigDecimal exchangeRate;
    private BigDecimal openingOriginalAmount;
    private BigDecimal openingAmount;
    private BigDecimal yearDebitOriginalAmount;
    private BigDecimal yearDebitAmount;
    private BigDecimal yearCreditOriginalAmount;
    private BigDecimal yearCreditAmount;
    @Schema(description = "科目期初余额（本位币，来自【期初余额】）")
    private BigDecimal subjectOpeningAmount;

}
