package cn.iocoder.yudao.module.fms.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - FMS 发票 Response VO")
@Data
public class FmsInvoiceRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "方向：1 销项 2 进项", example = "1")
    private Integer direction;

    @Schema(description = "发票种类：1 专票 2 普票", example = "1")
    private Integer invoiceType;

    @Schema(description = "发票代码")
    private String invoiceCode;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "开票日期")
    private LocalDate invoiceDate;

    @Schema(description = "开票月份 yyyy-MM", example = "2027-01")
    private String invoiceMonth;

    @Schema(description = "对方单位")
    private String counterpartName;

    @Schema(description = "对方税号")
    private String counterpartTaxNo;

    @Schema(description = "不含税金额")
    private BigDecimal amount;

    @Schema(description = "税率（百分数）")
    private BigDecimal taxRate;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "价税合计")
    private BigDecimal totalAmount;

    @Schema(description = "状态：0 正常 1 作废", example = "0")
    private Integer status;

    @Schema(description = "认证状态：0 未认证 1 已认证（只对进项专票有意义）", example = "0")
    private Integer certifyStatus;

    @Schema(description = "认证所属期 yyyy-MM")
    private String certifyPeriod;

    @Schema(description = "这张发票影响的月份已经生成了税务凭证，不能修改（先到增值税申报撤销）", example = "false")
    private Boolean locked;

    @Schema(description = "备注")
    private String remark;

}
