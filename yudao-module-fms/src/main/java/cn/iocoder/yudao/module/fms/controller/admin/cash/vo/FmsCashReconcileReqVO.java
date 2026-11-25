package cn.iocoder.yudao.module.fms.controller.admin.cash.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - FMS 出纳勾对 Request VO")
@Data
public class FmsCashReconcileReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "凭证分录编号数组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "凭证分录不能为空")
    private List<Long> entryIds;

    @Schema(description = "true 勾对 / false 取消勾对", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "勾对状态不能为空")
    private Boolean reconciled;

}
