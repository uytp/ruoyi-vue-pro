package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 外币余额表 Response VO")
@Data
public class FmsLedgerCurrencyBalanceRespVO {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long currencyId;
    private String currencyCode;
    private String currencyName;
    @Schema(description = "期初方向：借 / 贷 / 平")
    private String openingDirection;
    private BigDecimal openingOriginalAmount;
    private BigDecimal openingAmount;
    private BigDecimal periodDebitOriginalAmount;
    private BigDecimal periodDebitAmount;
    private BigDecimal periodCreditOriginalAmount;
    private BigDecimal periodCreditAmount;
    private BigDecimal yearDebitOriginalAmount;
    private BigDecimal yearDebitAmount;
    private BigDecimal yearCreditOriginalAmount;
    private BigDecimal yearCreditAmount;
    @Schema(description = "期末方向：借 / 贷 / 平")
    private String endingDirection;
    private BigDecimal endingOriginalAmount;
    private BigDecimal endingAmount;

}
