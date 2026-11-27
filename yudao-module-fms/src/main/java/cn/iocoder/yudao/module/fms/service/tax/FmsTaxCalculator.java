package cn.iocoder.yudao.module.fms.service.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 增值税与附加税的纯计算，不访问数据库，方便单独验证
 *
 * 规则（一般纳税人，按月申报）：
 * 1. 应纳税额 = 销项税额 - 本月可抵扣的进项税额 - 上期留抵税额；
 * 2. 结果为正，就是本月要交的增值税；结果为负，这个负数的绝对值就是留到下个月继续抵扣的“留抵税额”；
 * 3. 附加税（城建税、教育费附加、地方教育费附加）= 本月实际要交的增值税 × 各自税率，增值税为 0 或留抵时不用交。
 *
 * @author 芋道源码
 */
public final class FmsTaxCalculator {

    private FmsTaxCalculator() {
    }

    /**
     * 税额 = 不含税金额 × 税率（百分数），四舍五入保留两位
     */
    public static BigDecimal calcTax(BigDecimal amount, BigDecimal ratePercent) {
        return amount.multiply(ratePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * 增值税一个月的结果
     */
    public static final class Vat {
        /** 抵扣上期留抵后的净额：正数要交税，负数是留抵 */
        public final BigDecimal net;
        /** 本月应纳税额（>= 0） */
        public final BigDecimal payable;
        /** 期末留抵税额（>= 0），带到下个月 */
        public final BigDecimal carryEnd;

        Vat(BigDecimal net) {
            this.net = net;
            this.payable = net.signum() > 0 ? net : BigDecimal.ZERO;
            this.carryEnd = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
        }
    }

    public static Vat vat(BigDecimal outputTax, BigDecimal deductibleInputTax, BigDecimal priorCarry) {
        return new Vat(outputTax.subtract(deductibleInputTax).subtract(priorCarry));
    }

    /**
     * 附加税 = 应纳增值税 × 税率（百分数），四舍五入保留两位
     */
    public static BigDecimal surcharge(BigDecimal vatPayable, BigDecimal ratePercent) {
        if (vatPayable.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return vatPayable.multiply(ratePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

}
