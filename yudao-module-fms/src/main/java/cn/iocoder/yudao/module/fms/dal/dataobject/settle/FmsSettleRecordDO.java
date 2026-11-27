package cn.iocoder.yudao.module.fms.dal.dataobject.settle;

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
 * FMS 往来核销记录 DO
 *
 * 一条记录表示：某笔借方（应收增加 / 应付减少）和某笔贷方互相核销了多少钱。
 * 借方、贷方的来源用字符串表示：E+分录编号，或 O+科目编号-辅助组合编号（期初余额）。
 *
 * @author 芋道源码
 */
@TableName("fms_settle_record")
@KeySequence("fms_settle_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsSettleRecordDO extends BaseDO {

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
     * 核销批次号，同一次核销的记录共用
     */
    private Long batchId;
    /**
     * 往来科目编号
     */
    private Long subjectId;
    /**
     * 辅助核算类别编号
     */
    private Long auxiliaryTypeId;
    /**
     * 辅助核算项目编号
     */
    private Long auxiliaryItemId;
    /**
     * 借方来源
     */
    private String debitRef;
    /**
     * 贷方来源
     */
    private String creditRef;
    /**
     * 核销金额
     */
    private BigDecimal amount;
    /**
     * 核销日期
     */
    private LocalDate settleDate;
    /**
     * 备注
     */
    private String remark;

}
