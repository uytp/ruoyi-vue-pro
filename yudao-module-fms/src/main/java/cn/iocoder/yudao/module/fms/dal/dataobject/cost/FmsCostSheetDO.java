package cn.iocoder.yudao.module.fms.dal.dataobject.cost;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * FMS 月度成本计算单 DO
 */
@TableName("fms_cost_sheet")
@KeySequence("fms_cost_sheet_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsCostSheetDO extends BaseDO {

    /** 状态：草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 状态：已生成凭证 */
    public static final int STATUS_GENERATED = 1;

    @TableId
    private Long id;
    private Long accountSetId;
    /** 会计期间 yyyy-MM */
    private String period;
    /** 0草稿 1已生成凭证 */
    private Integer status;
    /** 本期制造费用归集总额 */
    private BigDecimal overheadTotal;
    /** 制造费用分配凭证 */
    private Long allocVoucherId;
    /** 完工入库凭证 */
    private Long doneVoucherId;
    /** 销售成本结转凭证 */
    private Long cogsVoucherId;

}
