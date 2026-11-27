package cn.iocoder.yudao.module.fms.dal.dataobject.cost;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * FMS 成本计算单明细 DO（每个产品一行）
 */
@TableName("fms_cost_sheet_item")
@KeySequence("fms_cost_sheet_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsCostSheetItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long sheetId;
    private Long accountSetId;
    /** 产品（辅助核算项目编号） */
    private Long productItemId;
    private String productCode;
    private String productName;
    private String unit;

    // ===== 手工录入 =====
    /** 生产工时 */
    private BigDecimal workHours;
    /** 本期完工数量 */
    private BigDecimal completedQty;
    /** 期末在产品数量 */
    private BigDecimal wipQty;
    /** 在产品完工程度（%） */
    private BigDecimal wipProgress;
    /** 本期销售数量 */
    private BigDecimal salesQty;
    /** 期初在产品-直接材料 */
    private BigDecimal beginWipMaterial;
    /** 期初在产品-直接人工 */
    private BigDecimal beginWipLabor;
    /** 期初在产品-制造费用 */
    private BigDecimal beginWipOverhead;
    /** 期初库存数量 */
    private BigDecimal beginStockQty;
    /** 期初库存金额 */
    private BigDecimal beginStockAmount;

    // ===== 计算结果 =====
    private BigDecimal curMaterial;
    private BigDecimal curLabor;
    private BigDecimal allocOverhead;
    private BigDecimal endWipMaterial;
    private BigDecimal endWipLabor;
    private BigDecimal endWipOverhead;
    private BigDecimal doneMaterial;
    private BigDecimal doneLabor;
    private BigDecimal doneOverhead;
    private BigDecimal doneTotal;
    private BigDecimal unitCost;
    private BigDecimal cogsAmount;
    private BigDecimal endStockQty;
    private BigDecimal endStockAmount;

}
