package cn.iocoder.yudao.module.fms.service.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 固定资产折旧的纯计算：年限平均法。不访问数据库，方便单独验证
 *
 * 规则（和小企业会计准则的通行做法一致）：
 * 1. 当月增加的资产，当月不提折旧，从下个月开始提；
 * 2. 月折旧额 = （原值 - 净残值）÷ 折旧月数，保留两位小数；
 * 3. 最后一个月不按公式算，直接把剩下没提完的全部提掉，这样累计折旧刚好等于“原值 - 净残值”，不会差几分钱；
 * 4. 当月减少（处置）的资产，当月仍然计提折旧。
 *
 * @author 芋道源码
 */
public final class FmsAssetCalculator {

    private FmsAssetCalculator() {
    }

    /**
     * 净残值 = 原值 × 净残值率（百分数）
     */
    public static BigDecimal residualValue(BigDecimal originalValue, BigDecimal residualRatePercent) {
        return originalValue.multiply(residualRatePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * 这个资产在 period 这个月是否开始计提：启用月份必须早于 period
     */
    public static boolean started(LocalDate startDate, YearMonth period) {
        return YearMonth.from(startDate).isBefore(period);
    }

    /**
     * 本月折旧额
     *
     * @param originalValue 原值
     * @param residualValue 净残值
     * @param lifeMonths 折旧月数
     * @param depreciatedMonths 已经折旧的月数（含进入系统前已提的月数）
     * @param accumDepr 已经累计的折旧（含进入系统前已提的金额）
     * @return 本月折旧额，已经提完时返回 0
     */
    public static BigDecimal monthlyAmount(BigDecimal originalValue, BigDecimal residualValue, int lifeMonths,
            int depreciatedMonths, BigDecimal accumDepr) {
        BigDecimal depreciable = originalValue.subtract(residualValue);
        BigDecimal left = depreciable.subtract(accumDepr);
        int remainMonths = lifeMonths - depreciatedMonths;
        if (remainMonths <= 0 || left.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (remainMonths == 1) {
            return left;
        }
        BigDecimal normal = depreciable.divide(BigDecimal.valueOf(lifeMonths), 2, RoundingMode.HALF_UP);
        return normal.min(left);
    }

    /**
     * 按公式算的月折旧额（给卡片列表显示用，不考虑最后一个月的差额）
     */
    public static BigDecimal standardMonthly(BigDecimal originalValue, BigDecimal residualValue, int lifeMonths) {
        if (lifeMonths <= 0) {
            return BigDecimal.ZERO;
        }
        return originalValue.subtract(residualValue).divide(BigDecimal.valueOf(lifeMonths), 2, RoundingMode.HALF_UP);
    }

}
