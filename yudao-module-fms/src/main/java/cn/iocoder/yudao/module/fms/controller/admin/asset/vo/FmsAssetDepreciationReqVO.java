package cn.iocoder.yudao.module.fms.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 固定资产折旧计提 / 撤销 Request VO")
@Data
public class FmsAssetDepreciationReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "会计期间 yyyy-MM", requiredMode = Schema.RequiredMode.REQUIRED, example = "2027-01")
    @NotNull(message = "会计期间不能为空")
    private String period;

}
