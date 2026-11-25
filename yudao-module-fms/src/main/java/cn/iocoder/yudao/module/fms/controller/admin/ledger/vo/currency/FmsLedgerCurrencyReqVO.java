package cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - FMS 外币账簿查询 Request VO")
@Data
public class FmsLedgerCurrencyReqVO {

    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;
    @NotNull(message = "开始会计期间不能为空")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "开始会计期间格式不正确")
    private String startMonth;
    @NotNull(message = "结束会计期间不能为空")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "结束会计期间格式不正确")
    private String endMonth;
    @Schema(description = "科目编号")
    private Long subjectId;
    @Schema(description = "币别编号")
    private Long currencyId;

}
