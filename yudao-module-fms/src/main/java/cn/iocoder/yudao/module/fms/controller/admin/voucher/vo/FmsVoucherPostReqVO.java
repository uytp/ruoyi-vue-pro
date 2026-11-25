package cn.iocoder.yudao.module.fms.controller.admin.voucher.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - FMS 凭证过账 Request VO")
@Data
public class FmsVoucherPostReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "账套不能为空")
    private Long accountSetId;

    @Schema(description = "凭证编号数组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "凭证编号不能为空")
    private List<Long> ids;

    @Schema(description = "是否过账：true 过账，false 反过账", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "过账状态不能为空")
    private Boolean posted;

}
