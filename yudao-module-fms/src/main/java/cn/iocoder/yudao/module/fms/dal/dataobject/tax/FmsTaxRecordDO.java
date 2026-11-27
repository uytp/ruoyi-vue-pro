package cn.iocoder.yudao.module.fms.dal.dataobject.tax;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * FMS 税务凭证记录 DO：某个月生成过税务结转凭证就有一条
 *
 * @author 芋道源码
 */
@TableName("fms_tax_record")
@KeySequence("fms_tax_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsTaxRecordDO extends BaseDO {

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
     * 税款所属期 yyyy-MM
     */
    private String period;
    /**
     * 生成凭证时的应纳增值税
     */
    private BigDecimal vatPayable;
    /**
     * 生成凭证时的附加税合计
     */
    private BigDecimal surchargeTotal;
    /**
     * 税务结转凭证
     */
    private Long voucherId;

}
