package cn.iocoder.yudao.module.fms.dal.dataobject.cost;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * FMS 成本参数 DO（每个账套一条）
 */
@TableName("fms_cost_setting")
@KeySequence("fms_cost_setting_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsCostSettingDO extends BaseDO {

    @TableId
    private Long id;
    /** 账套编号 */
    private Long accountSetId;
    /** 产品对应的辅助核算类别（默认存货） */
    private Long productAuxTypeId;
    /** 生产成本-直接材料 */
    private Long materialSubjectId;
    /** 生产成本-直接人工 */
    private Long laborSubjectId;
    /** 生产成本-制造费用（分配转入） */
    private Long overheadInSubjectId;
    /** 制造费用（归集池） */
    private Long overheadPoolSubjectId;
    /** 库存商品 */
    private Long finishedSubjectId;
    /** 主营业务成本 */
    private Long cogsSubjectId;
    /** 制造费用分配方法：1完工数量 2生产工时 3直接人工 4直接成本 */
    private Integer overheadAllocMethod;
    /** 在产品：0不计算在产品 1约当产量法 */
    private Integer wipMethod;
    /** 材料投入：1开工一次投入 2随加工进度投入 */
    private Integer materialInputMode;

}
