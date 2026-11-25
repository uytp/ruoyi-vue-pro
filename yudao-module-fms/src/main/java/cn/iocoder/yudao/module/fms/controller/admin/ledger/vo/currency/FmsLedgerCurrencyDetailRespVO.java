package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 外币明细账 Response VO")
@Data
public class FmsLedgerCurrencyDetailRespVO {

    @Schema(description = "行类型：1 期初余额 2 凭证分录 3 本月合计 4 本年累计")
    private Integer rowType;
    private String period;
    private String accountDate;
    private Long voucherId;
    private String voucherNumber;
    private String digest;
    private BigDecimal exchangeRate;
    private BigDecimal debitOriginalAmount;
    private BigDecimal debitAmount;
    private BigDecimal creditOriginalAmount;
    private BigDecimal creditAmount;
    @Schema(description = "余额方向：借 / 贷 / 平")
    private String balanceDirection;
    private BigDecimal balanceOriginalAmount;
    private BigDecimal balanceAmount;

}
