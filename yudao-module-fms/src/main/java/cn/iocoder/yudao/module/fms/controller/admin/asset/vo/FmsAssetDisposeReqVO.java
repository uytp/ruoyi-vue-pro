package cn.iocoder.yudao.module.fms.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - FMS 固定资产处置 Request VO")
@Data
public class FmsAssetDisposeReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "资产卡片编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产不能为空")
    private Long id;

    @Schema(description = "处置日期，同时是转入清理凭证的日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2027-02-10")
    @NotNull(message = "处置日期不能为空")
    private LocalDate disposeDate;

    @Schema(description = "备注，会写进凭证摘要后面")
    @Size(max = 100, message = "备注不能超过 100 个字")
    private String remark;

}
