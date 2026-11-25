package cn.iocoder.yudao.module.fms.dal.dataobject.config;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * FMS 外币期初余额 DO：金额均按科目余额方向记为正数
 */
@TableName("fms_initial_currency_balance")
@KeySequence("fms_initial_currency_balance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsInitialCurrencyBalanceDO extends BaseDO {

    @TableId
    private Long id;
    private Long accountSetId;
    private Long subjectId;
    private Long currencyId;
    /**
     * 期初原币
     */
    private BigDecimal openingOriginalAmount;
    /**
     * 期初本位币
     */
    private BigDecimal openingAmount;
    /**
     * 本年累计借方原币
     */
    private BigDecimal yearDebitOriginalAmount;
    /**
     * 本年累计借方本位币
     */
    private BigDecimal yearDebitAmount;
    /**
     * 本年累计贷方原币
     */
    private BigDecimal yearCreditOriginalAmount;
    /**
     * 本年累计贷方本位币
     */
    private BigDecimal yearCreditAmount;

}
