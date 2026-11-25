package cn.iocoder.yudao.module.fms.dal.dataobject.cash;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * FMS 银行余额调节表 DO
 *
 * @author 芋道源码
 */
@TableName("fms_bank_reconcile")
@KeySequence("fms_bank_reconcile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsBankReconcileDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 账套编号
     */
    private Long accountSetId;
    /**
     * 银行存款科目编号
     */
    private Long subjectId;
    /**
     * 会计期间，格式 yyyy-MM
     */
    private String month;
    /**
     * 日记账余额
     */
    private BigDecimal bookBalance;
    /**
     * 银行对账单余额
     */
    private BigDecimal bankBalance;
    /**
     * 加：企业已收银行未收
     */
    private BigDecimal bankPlusDepositInTransit;
    /**
     * 减：企业已付银行未付
     */
    private BigDecimal bankLessOutstandingCheck;
    /**
     * 加：银行已收企业未收
     */
    private BigDecimal bookPlusBankCollection;
    /**
     * 减：银行已付企业未付
     */
    private BigDecimal bookLessBankPayment;
    /**
     * 调节后余额
     */
    private BigDecimal adjustedBalance;
    /**
     * 状态：0 未调节 1 已调节
     */
    private Integer status;

}
