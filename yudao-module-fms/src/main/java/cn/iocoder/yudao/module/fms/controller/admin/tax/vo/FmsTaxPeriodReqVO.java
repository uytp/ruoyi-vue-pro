package cn.iocoder.yudao.module.fms.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 税务凭证生成 / 撤销 Request VO")
@Data
public class FmsTaxPeriodReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "税款所属期 yyyy-MM", requiredMode = Schema.RequiredMode.REQUIRED, example = "2027-01")
    @NotNull(message = "税款所属期不能为空")
    private String period;

}
