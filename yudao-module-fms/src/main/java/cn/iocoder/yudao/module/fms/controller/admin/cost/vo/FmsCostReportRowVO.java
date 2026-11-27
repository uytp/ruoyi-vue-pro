package cn.iocoder.yudao.module.fms.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成本报表的一行：汇总表里 period 为空，明细表里 period 是具体月份
 */
@Schema(description = "管理后台 - FMS 成本报表行 VO")
@Data
public class FmsCostReportRowVO {

    @Schema(description = "会计期间 yyyy-MM（汇总行为空）")
    private String period;
    @Schema(description = "成本单状态：0草稿 1已生成凭证（汇总行为空）")
    private Integer status;

    @Schema(description = "产品编号")
    private Long productItemId;
    @Schema(description = "产品编码")
    private String productCode;
    @Schema(description = "产品名称")
    private String productName;
    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "完工数量")
    private BigDecimal completedQty;
    @Schema(description = "完工成本-直接材料")
    private BigDecimal doneMaterial;
    @Schema(description = "完工成本-直接人工")
    private BigDecimal doneLabor;
    @Schema(description = "完工成本-制造费用")
    private BigDecimal doneOverhead;
    @Schema(description = "完工成本合计")
    private BigDecimal doneTotal;
    @Schema(description = "单位成本（完工成本合计 ÷ 完工数量）")
    private BigDecimal unitCost;
    @Schema(description = "直接材料占比（%）")
    private BigDecimal materialRate;
    @Schema(description = "直接人工占比（%）")
    private BigDecimal laborRate;
    @Schema(description = "制造费用占比（%）")
    private BigDecimal overheadRate;

    @Schema(description = "销售数量")
    private BigDecimal salesQty;
    @Schema(description = "销售成本")
    private BigDecimal cogsAmount;

    @Schema(description = "期末在产品成本（汇总行取查询范围内最后一个月）")
    private BigDecimal endWipTotal;
    @Schema(description = "期末库存数量（汇总行取查询范围内最后一个月）")
    private BigDecimal endStockQty;
    @Schema(description = "期末库存金额（汇总行取查询范围内最后一个月）")
    private BigDecimal endStockAmount;

}
