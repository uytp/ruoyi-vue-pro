package cn.iocoder.yudao.module.fms.dal.dataobject.asset;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * FMS 固定资产类别 DO
 *
 * 一个类别定义这类资产默认的折旧年限、净残值率，以及入账用到的三个科目
 *
 * @author 芋道源码
 */
@TableName("fms_asset_category")
@KeySequence("fms_asset_category_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsAssetCategoryDO extends BaseDO {

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
     * 类别编码
     */
    private String code;
    /**
     * 类别名称
     */
    private String name;
    /**
     * 默认折旧月数
     */
    private Integer lifeMonths;
    /**
     * 默认净残值率（百分数，5 表示 5%）
     */
    private BigDecimal residualRate;
    /**
     * 固定资产科目
     */
    private Long assetSubjectId;
    /**
     * 累计折旧科目
     */
    private Long accumSubjectId;
    /**
     * 折旧费用科目
     */
    private Long expenseSubjectId;
    /**
     * 备注
     */
    private String remark;

}
