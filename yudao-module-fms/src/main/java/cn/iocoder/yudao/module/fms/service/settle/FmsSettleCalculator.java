package cn.iocoder.yudao.module.fms.service.settle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.SETTLE_BUCKET_INVALID;

/**
 * 往来核销与账龄的纯计算：账龄区间、核销配对、自动匹配。不访问数据库，方便单独验证
 *
 * @author 芋道源码
 */
public final class FmsSettleCalculator {

    /** 默认账龄区间（天） */
    public static final String DEFAULT_BUCKETS = "30,60,90,180,365";

    private FmsSettleCalculator() {
    }

    /**
     * 一笔往来项目：来自一条凭证分录，或来自期初余额。金额一律为正数，方向用 debit 表示
     */
    public static class Item {
        /** E+分录编号，或 O+科目编号-辅助组合编号 */
        public String ref;
        public boolean opening;
        public Long voucherId;
        public String voucherNo;
        public LocalDate date;
        public String digest;
        /** true 借方，false 贷方 */
        public boolean debit;
        public BigDecimal amount = BigDecimal.ZERO;
        public BigDecimal settled = BigDecimal.ZERO;
        public BigDecimal remain = BigDecimal.ZERO;
    }

    /** 一次配对：借方 ref 和贷方 ref 互相核销 amount */
    public static class Pair {
        public final String debitRef;
        public final String creditRef;
        public final BigDecimal amount;

        public Pair(String debitRef, String creditRef, BigDecimal amount) {
            this.debitRef = debitRef;
            this.creditRef = creditRef;
            this.amount = amount;
        }
    }

    /** 核销请求里的一行：来源 + 本次核销金额 */
    public static class Line {
        public final String ref;
        public final BigDecimal amount;

        public Line(String ref, BigDecimal amount) {
            this.ref = ref;
            this.amount = amount;
        }
    }

    /**
     * 解析账龄区间，例如 "30,60,90" 表示 0-30、31-60、61-90、90 以上
     */
    public static List<Integer> parseBuckets(String text) {
        String value = text == null || text.isBlank() ? DEFAULT_BUCKETS : text.trim();
        List<Integer> result = new ArrayList<>();
        try {
            for (String part : value.replace('，', ',').split(",")) {
                int day = Integer.parseInt(part.trim());
                if (day <= 0 || (!result.isEmpty() && day <= result.get(result.size() - 1))) {
                    throw exception(SETTLE_BUCKET_INVALID);
                }
                result.add(day);
            }
        } catch (NumberFormatException e) {
            throw exception(SETTLE_BUCKET_INVALID);
        }
        if (result.size() > 8) {
            throw exception(SETTLE_BUCKET_INVALID);
        }
        return result;
    }

    /**
     * 账龄区间名称：0-30天、31-60天 … 365天以上
     */
    public static List<String> bucketLabels(List<Integer> bounds) {
        List<String> labels = new ArrayList<>();
        int previous = 0;
        for (int i = 0; i < bounds.size(); i++) {
            int bound = bounds.get(i);
            labels.add((i == 0 ? 0 : previous + 1) + "-" + bound + "天");
            previous = bound;
        }
        labels.add(previous + "天以上");
        return labels;
    }

    /**
     * 账龄天数落在第几个区间（从 0 开始）
     */
    public static int bucketIndex(int ageDays, List<Integer> bounds) {
        for (int i = 0; i < bounds.size(); i++) {
            if (ageDays <= bounds.get(i)) {
                return i;
            }
        }
        return bounds.size();
    }

    /**
     * 账龄天数：截止日期减去凭证日期，不会出现负数
     */
    public static int ageDays(LocalDate itemDate, LocalDate asOfDate) {
        return (int) Math.max(0, ChronoUnit.DAYS.between(itemDate, asOfDate));
    }

    /**
     * 把本次选中的借方行和贷方行配对。借贷合计必须相等（调用前已校验），
     * 按顺序一笔笔对冲：先把第一笔借方和第一笔贷方对冲，谁先用完就换下一笔
     */
    public static List<Pair> allocate(List<Line> debits, List<Line> credits) {
        List<Pair> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        BigDecimal restDebit = debits.isEmpty() ? BigDecimal.ZERO : debits.get(0).amount;
        BigDecimal restCredit = credits.isEmpty() ? BigDecimal.ZERO : credits.get(0).amount;
        while (i < debits.size() && j < credits.size()) {
            BigDecimal amount = restDebit.min(restCredit);
            if (amount.signum() > 0) {
                result.add(new Pair(debits.get(i).ref, credits.get(j).ref, amount));
            }
            restDebit = restDebit.subtract(amount);
            restCredit = restCredit.subtract(amount);
            if (restDebit.signum() == 0) {
                i++;
                if (i < debits.size()) {
                    restDebit = debits.get(i).amount;
                }
            }
            if (restCredit.signum() == 0) {
                j++;
                if (j < credits.size()) {
                    restCredit = credits.get(j).amount;
                }
            }
        }
        return result;
    }

    /**
     * 自动匹配：借方、贷方未核销余额完全相等的，一对一核销。两边都按日期从早到晚排好序传入，
     * 每笔借方找到第一笔还没用过、余额相等的贷方
     */
    public static List<Pair> autoMatch(List<Item> debits, List<Item> credits) {
        List<Pair> result = new ArrayList<>();
        boolean[] used = new boolean[credits.size()];
        for (Item debit : debits) {
            if (debit.remain.signum() <= 0) {
                continue;
            }
            for (int k = 0; k < credits.size(); k++) {
                Item credit = credits.get(k);
                if (!used[k] && credit.remain.signum() > 0 && credit.remain.compareTo(debit.remain) == 0) {
                    used[k] = true;
                    result.add(new Pair(debit.ref, credit.ref, debit.remain));
                    break;
                }
            }
        }
        return result;
    }

}
