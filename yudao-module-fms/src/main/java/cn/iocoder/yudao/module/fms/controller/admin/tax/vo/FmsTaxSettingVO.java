package cn.iocoder.yudao.module.fms.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - FMS 税务参数 Request / Response VO")
@Data
public class FmsTaxSettingVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "开始用本系统算增值税的月份 yyyy-MM，不填取账套启用月份", example = "2027-01")
    private String startMonth;

    @Schema(description = "开始月份的上期留抵税额", example = "0.00")
    private BigDecimal initCarry;

    @Schema(description = "城市维护建设税税率（百分数）", example = "7")
    private BigDecimal urbanRate;

    @Schema(description = "教育费附加征收率（百分数）", example = "3")
    private BigDecimal eduRate;

    @Schema(description = "地方教育费附加征收率（百分数）", example = "2")
    private BigDecimal localEduRate;

    @Schema(description = "实际使用的开始月份（只读，返回时才有）", example = "2026-11")
    private String effectiveStartMonth;

}
