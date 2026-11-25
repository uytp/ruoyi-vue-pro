package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 币别汇率保存 Request VO")
@Data
public class FmsCurrencyRateSaveReqVO {

    @Schema(description = "汇率记录编号")
    private Long id;
    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;
    @Schema(description = "币别编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "币别不能为空")
    private Long currencyId;
    @Schema(description = "会计期间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-12")
    @NotNull(message = "会计期间不能为空")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "会计期间格式不正确")
    private String month;
    @Schema(description = "汇率", requiredMode = Schema.RequiredMode.REQUIRED, example = "7.100000")
    @NotNull(message = "汇率不能为空")
    @DecimalMin(value = "0.000001", message = "汇率必须大于 0")
    @Digits(integer = 12, fraction = 6, message = "汇率最多保留 6 位小数")
    private BigDecimal rate;

}
