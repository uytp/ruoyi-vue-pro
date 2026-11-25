package cn.iocoder.yudao.module.fms.dal.dataobject.cash;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FMS 银行对账单 DO
 *
 * @author 芋道源码
 */
@TableName("fms_bank_statement")
@KeySequence("fms_bank_statement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsBankStatementDO extends BaseDO {

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
     * 交易日期
     */
    private LocalDate statementDate;
    /**
     * 摘要
     */
    private String summary;
    /**
     * 收入金额（银行借方）
     */
    private BigDecimal debitAmount;
    /**
     * 支出金额（银行贷方）
     */
    private BigDecimal creditAmount;
    /**
     * 银行余额
     */
    private BigDecimal balance;
    /**
     * 银行流水号，用于去重
     */
    private String referenceNo;
    /**
     * 导入批次号
     */
    private String importBatch;
    /**
     * 是否已勾对
     */
    private Boolean reconciled;
    /**
     * 勾对的凭证分录编号
     */
    private Long voucherEntryId;

}
