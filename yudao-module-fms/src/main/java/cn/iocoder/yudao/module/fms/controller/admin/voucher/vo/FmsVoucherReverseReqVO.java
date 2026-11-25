package cn.iocoder.yudao.module.fms.controller.admin.voucher.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 凭证红字冲销 Request VO")
@Data
public class FmsVoucherReverseReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "账套不能为空")
    private Long accountSetId;

    @Schema(description = "要冲销的原凭证编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "凭证编号不能为空")
    private Long id;

}
