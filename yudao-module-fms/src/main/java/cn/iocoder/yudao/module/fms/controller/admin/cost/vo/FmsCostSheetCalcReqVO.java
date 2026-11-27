package cn.iocoder.yudao.module.fms.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - FMS 成本计算 Request VO")
@Data
public class FmsCostSheetCalcReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套不能为空")
    private Long accountSetId;

    @Schema(description = "会计期间 yyyy-MM", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-12")
    @NotBlank(message = "会计期间不能为空")
    private String period;

    @Schema(description = "各产品的手工录入数据；不传则沿用上次保存的录入")
    @Valid
    private List<FmsCostSheetItemVO> items;

}
