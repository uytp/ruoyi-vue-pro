package cn.iocoder.yudao.module.fms.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - FMS 发票保存 Request VO")
@Data
public class FmsInvoiceSaveReqVO {

    @Schema(description = "编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "方向：1 销项（开出去的） 2 进项（收进来的）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "方向不能为空")
    private Integer direction;

    @Schema(description = "发票种类：1 增值税专用发票 2 增值税普通发票，默认 1", example = "1")
    private Integer invoiceType;

    @Schema(description = "发票代码", example = "044001900111")
    @Size(max = 32, message = "发票代码不能超过 32 个字")
    private String invoiceCode;

    @Schema(description = "发票号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345678")
    @NotBlank(message = "发票号码不能为空")
    @Size(max = 32, message = "发票号码不能超过 32 个字")
    private String invoiceNo;

    @Schema(description = "开票日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2027-01-15")
    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;

    @Schema(description = "对方单位（销项填购买方，进项填销售方）", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户甲")
    @NotBlank(message = "对方单位不能为空")
    @Size(max = 128, message = "对方单位不能超过 128 个字")
    private String counterpartName;

    @Schema(description = "对方税号")
    @Size(max = 32, message = "对方税号不能超过 32 个字")
    private String counterpartTaxNo;

    @Schema(description = "不含税金额，红字发票填负数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @NotNull(message = "不含税金额不能为空")
    private BigDecimal amount;

    @Schema(description = "税率（百分数，13 表示 13%）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13")
    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", message = "税率不能小于 0")
    @DecimalMax(value = "100", message = "税率不能大于 100")
    private BigDecimal taxRate;

    @Schema(description = "税额；不填按“金额 × 税率”自动算，填了会和自动算的比对（相差不超过 0.10 元）", example = "1300.00")
    private BigDecimal taxAmount;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注不能超过 255 个字")
    private String remark;

}
