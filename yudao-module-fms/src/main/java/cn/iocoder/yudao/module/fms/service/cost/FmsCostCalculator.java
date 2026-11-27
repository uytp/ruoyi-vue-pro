package cn.iocoder.yudao.module.fms.service.cost;

import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSheetItemDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 成本计算的纯计算部分（不访问数据库，方便单独验证）
 */
final class FmsCostCalculator {

    static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 分配方法：完工数量 */
    static final int ALLOC_BY_COMPLETED_QTY = 1;
    /** 分配方法：生产工时 */
    static final int ALLOC_BY_WORK_HOURS = 2;
    /** 分配方法：直接人工 */
    static final int ALLOC_BY_LABOR = 3;
    /** 分配方法：直接成本（直接材料 + 直接人工） */
    static final int ALLOC_BY_DIRECT_COST = 4;

    private FmsCostCalculator() {
    }

    static BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    static String allocBaseName(int method) {
        return switch (method) {
            case ALLOC_BY_WORK_HOURS -> "生产工时";
            case ALLOC_BY_LABOR -> "直接人工";
            case ALLOC_BY_DIRECT_COST -> "直接成本";
            default -> "完工数量";
        };
    }

    /**
     * 取产品的制造费用分配标准
     */
    static BigDecimal allocBase(FmsCostSheetItemDO item, int method) {
        return switch (method) {
            case ALLOC_BY_WORK_HOURS -> nvl(item.getWorkHours());
            case ALLOC_BY_LABOR -> nvl(item.getCurLabor());
            case ALLOC_BY_DIRECT_COST -> nvl(item.getCurMaterial()).add(nvl(item.getCurLabor()));
            default -> nvl(item.getCompletedQty());
        };
    }

    /**
     * 把本期制造费用按标准分配给各产品；尾差放在最后一个有标准的产品上，保证分配合计等于归集总额
     *
     * @return 分配标准合计为 0 且需要分配的金额不为 0 时返回 false（调用方应提示用户先填写标准）
     */
    static boolean allocateOverhead(List<FmsCostSheetItemDO> items, BigDecimal pool, int method) {
        items.forEach(item -> item.setAllocOverhead(ZERO));
        if (pool.signum() == 0) {
            return true;
        }
        BigDecimal totalBase = ZERO;
        FmsCostSheetItemDO last = null;
        for (FmsCostSheetItemDO item : items) {
            BigDecimal base = allocBase(item, method);
            if (base.signum() > 0) {
                totalBase = totalBase.add(base);
                last = item;
            }
        }
        if (totalBase.signum() == 0) {
            return false;
        }
        BigDecimal allocated = ZERO;
        for (FmsCostSheetItemDO item : items) {
            BigDecimal base = allocBase(item, method);
            if (base.signum() <= 0 || item == last) {
                continue;
            }
            BigDecimal amount = pool.multiply(base).divide(totalBase, 2, RoundingMode.HALF_UP);
            item.setAllocOverhead(amount);
            allocated = allocated.add(amount);
        }
        last.setAllocOverhead(pool.subtract(allocated));
        return true;
    }

    /**
     * 计算单个产品：完工产品成本、期末在产品成本、单位成本，以及月末一次加权平均的销售成本和期末库存
     *
     * @param wipMethod         在产品处理方式：0 不计算在产品（全部完工）、1 约当产量法
     * @param materialInputMode 材料投入方式：1 开工一次投入、2 随加工进度投入
     * @return 销售数量超过可售数量时返回可售数量，否则返回 null
     */
    static BigDecimal computeItem(FmsCostSheetItemDO item, int wipMethod, int materialInputMode) {
        BigDecimal totalMaterial = nvl(item.getBeginWipMaterial()).add(nvl(item.getCurMaterial()));
        BigDecimal totalLabor = nvl(item.getBeginWipLabor()).add(nvl(item.getCurLabor()));
        BigDecimal totalOverhead = nvl(item.getBeginWipOverhead()).add(nvl(item.getAllocOverhead()));
        BigDecimal completedQty = nvl(item.getCompletedQty());
        BigDecimal wipQty = nvl(item.getWipQty());

        BigDecimal endWipMaterial;
        BigDecimal endWipLabor;
        BigDecimal endWipOverhead;
        if (completedQty.signum() <= 0) {
            // 本期没有完工产品：费用全部留在在产品里，结转到下期
            endWipMaterial = totalMaterial;
            endWipLabor = totalLabor;
            endWipOverhead = totalOverhead;
        } else if (wipMethod != 1 || wipQty.signum() <= 0) {
            endWipMaterial = ZERO;
            endWipLabor = ZERO;
            endWipOverhead = ZERO;
        } else {
            BigDecimal progress = nvl(item.getWipProgress()).divide(HUNDRED, 6, RoundingMode.HALF_UP)
                    .max(ZERO).min(BigDecimal.ONE);
            BigDecimal equivalentProcess = wipQty.multiply(progress);
            BigDecimal equivalentMaterial = materialInputMode == 1 ? wipQty : equivalentProcess;
            endWipMaterial = shareOfWip(totalMaterial, completedQty, equivalentMaterial);
            endWipLabor = shareOfWip(totalLabor, completedQty, equivalentProcess);
            endWipOverhead = shareOfWip(totalOverhead, completedQty, equivalentProcess);
        }
        item.setEndWipMaterial(endWipMaterial);
        item.setEndWipLabor(endWipLabor);
        item.setEndWipOverhead(endWipOverhead);
        item.setDoneMaterial(totalMaterial.subtract(endWipMaterial));
        item.setDoneLabor(totalLabor.subtract(endWipLabor));
        item.setDoneOverhead(totalOverhead.subtract(endWipOverhead));
        BigDecimal doneTotal = item.getDoneMaterial().add(item.getDoneLabor()).add(item.getDoneOverhead());
        item.setDoneTotal(doneTotal);
        item.setUnitCost(completedQty.signum() > 0
                ? doneTotal.divide(completedQty, 6, RoundingMode.HALF_UP) : ZERO);

        // 销售成本：月末一次加权平均 = （期初库存金额 + 本期完工成本）× 销售数量 ÷ （期初库存数量 + 本期完工数量）
        BigDecimal availableQty = nvl(item.getBeginStockQty()).add(completedQty);
        BigDecimal availableAmount = nvl(item.getBeginStockAmount()).add(doneTotal);
        BigDecimal salesQty = nvl(item.getSalesQty());
        if (salesQty.compareTo(availableQty) > 0) {
            return availableQty;
        }
        BigDecimal cogs;
        if (salesQty.signum() == 0 || availableQty.signum() == 0) {
            cogs = ZERO;
        } else if (salesQty.compareTo(availableQty) == 0) {
            cogs = availableAmount;
        } else {
            cogs = availableAmount.multiply(salesQty).divide(availableQty, 2, RoundingMode.HALF_UP);
        }
        item.setCogsAmount(cogs);
        item.setEndStockQty(availableQty.subtract(salesQty));
        item.setEndStockAmount(availableAmount.subtract(cogs));
        return null;
    }

    /**
     * 约当产量法：在产品应负担的费用 = 费用总额 × 在产品约当量 ÷ （完工数量 + 在产品约当量）
     */
    private static BigDecimal shareOfWip(BigDecimal total, BigDecimal completedQty, BigDecimal equivalentQty) {
        BigDecimal denominator = completedQty.add(equivalentQty);
        if (total.signum() == 0 || denominator.signum() == 0 || equivalentQty.signum() == 0) {
            return ZERO;
        }
        return total.multiply(equivalentQty).divide(denominator, 2, RoundingMode.HALF_UP);
    }

}
