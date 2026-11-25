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
 * FMS 币别汇率表 DO：按会计期间记录记账 / 期末调汇汇率（浮动汇率）
 */
@TableName("fms_currency_rate")
@KeySequence("fms_currency_rate_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsCurrencyRateDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 账套编号
     */
    private Long accountSetId;
    /**
     * 币别编号
     */
    private Long currencyId;
    /**
     * 会计期间，格式 yyyy-MM
     */
    private String month;
    /**
     * 汇率（1 外币 = rate 本位币）
     */
    private BigDecimal rate;

}
