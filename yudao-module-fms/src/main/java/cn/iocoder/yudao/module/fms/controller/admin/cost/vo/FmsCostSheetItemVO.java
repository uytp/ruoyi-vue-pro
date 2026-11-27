package cn.iocoder.yudao.module.fms.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - FMS 成本计算单明细 VO（录入与结果共用，录入时只读取“手工录入”部分）")
@Data
public class FmsCostSheetItemVO {

    @Schema(description = "产品（辅助核算项目）编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productItemId;
    @Schema(description = "产品编码")
    private String productCode;
    @Schema(description = "产品名称")
    private String productName;
    @Schema(description = "计量单位")
    private String unit;

    // ===== 手工录入 =====
    @Schema(description = "生产工时")
    @DecimalMin(value = "0", message = "生产工时不能小于 0")
    private BigDecimal workHours;
    @Schema(description = "本期完工数量")
    @DecimalMin(value = "0", message = "本期完工数量不能小于 0")
    private BigDecimal completedQty;
    @Schema(description = "期末在产品数量")
    @DecimalMin(value = "0", message = "期末在产品数量不能小于 0")
    private BigDecimal wipQty;
    @Schema(description = "在产品完工程度（%）")
    @DecimalMin(value = "0", message = "完工程度不能小于 0")
    private BigDecimal wipProgress;
    @Schema(description = "本期销售数量")
    @DecimalMin(value = "0", message = "本期销售数量不能小于 0")
    private BigDecimal salesQty;
    @Schema(description = "期初在产品-直接材料（上月有成本单的产品自动带入，不能改；第一次使用或新产品才能手工录入）")
    @DecimalMin(value = "0", message = "期初在产品-直接材料不能小于 0")
    private BigDecimal beginWipMaterial;
    @Schema(description = "期初在产品-直接人工")
    @DecimalMin(value = "0", message = "期初在产品-直接人工不能小于 0")
    private BigDecimal beginWipLabor;
    @Schema(description = "期初在产品-制造费用")
    @DecimalMin(value = "0", message = "期初在产品-制造费用不能小于 0")
    private BigDecimal beginWipOverhead;
    @Schema(description = "期初库存数量")
    @DecimalMin(value = "0", message = "期初库存数量不能小于 0")
    private BigDecimal beginStockQty;
    @Schema(description = "期初库存金额")
    @DecimalMin(value = "0", message = "期初库存金额不能小于 0")
    private BigDecimal beginStockAmount;

    @Schema(description = "期初数是否由上月成本单自动带入（true 时期初数不能手工改）；只在返回结果里有意义")
    private Boolean beginCarried;

    // ===== 计算结果 =====
    @Schema(description = "本期直接材料（从凭证归集）")
    private BigDecimal curMaterial;
    @Schema(description = "本期直接人工（从凭证归集）")
    private BigDecimal curLabor;
    @Schema(description = "本期分配制造费用")
    private BigDecimal allocOverhead;
    @Schema(description = "期末在产品-直接材料")
    private BigDecimal endWipMaterial;
    @Schema(description = "期末在产品-直接人工")
    private BigDecimal endWipLabor;
    @Schema(description = "期末在产品-制造费用")
    private BigDecimal endWipOverhead;
    @Schema(description = "完工产品-直接材料")
    private BigDecimal doneMaterial;
    @Schema(description = "完工产品-直接人工")
    private BigDecimal doneLabor;
    @Schema(description = "完工产品-制造费用")
    private BigDecimal doneOverhead;
    @Schema(description = "完工产品总成本")
    private BigDecimal doneTotal;
    @Schema(description = "完工产品单位成本")
    private BigDecimal unitCost;
    @Schema(description = "本期销售成本")
    private BigDecimal cogsAmount;
    @Schema(description = "期末库存数量")
    private BigDecimal endStockQty;
    @Schema(description = "期末库存金额")
    private BigDecimal endStockAmount;

}
