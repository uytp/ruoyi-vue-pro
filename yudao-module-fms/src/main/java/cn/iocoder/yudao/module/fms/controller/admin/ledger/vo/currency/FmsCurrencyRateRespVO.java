package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 币别汇率 Response VO")
@Data
public class FmsCurrencyRateRespVO {

    @Schema(description = "汇率记录编号；为空表示取币别默认汇率")
    private Long id;
    @Schema(description = "币别编号")
    private Long currencyId;
    @Schema(description = "币别编码")
    private String currencyCode;
    @Schema(description = "币别名称")
    private String currencyName;
    @Schema(description = "会计期间", example = "2026-12")
    private String month;
    @Schema(description = "汇率")
    private BigDecimal rate;
    @Schema(description = "汇率来源：1 本期汇率表 2 沿用以前期间 3 币别默认汇率")
    private Integer source;

}
