package cn.iocoder.yudao.module.fms.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - FMS 发票认证 / 作废 Request VO")
@Data
public class FmsInvoiceBatchReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "发票编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请先选择发票")
    private List<Long> ids;

    @Schema(description = "认证：true 认证 false 取消认证；作废：true 作废 false 恢复", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "操作不能为空")
    private Boolean flag;

    @Schema(description = "认证所属期 yyyy-MM（只在认证时用，不填取开票月份）", example = "2027-01")
    private String certifyPeriod;

}
