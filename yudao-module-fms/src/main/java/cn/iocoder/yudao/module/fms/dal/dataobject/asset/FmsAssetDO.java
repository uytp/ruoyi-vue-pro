package cn.iocoder.yudao.module.fms.dal.dataobject.asset;

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
 * FMS 固定资产卡片 DO
 *
 * @author 芋道源码
 */
@TableName("fms_asset")
@KeySequence("fms_asset_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsAssetDO extends BaseDO {

    /** 状态：在用 */
    public static final int STATUS_IN_USE = 0;
    /** 状态：已处置 */
    public static final int STATUS_DISPOSED = 1;

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
     * 资产编号
     */
    private String code;
    /**
     * 资产名称
     */
    private String name;
    /**
     * 资产类别
     */
    private Long categoryId;
    /**
     * 规格型号
     */
    private String spec;
    /**
     * 计量单位
     */
    private String unit;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 购入日期
     */
    private LocalDate purchaseDate;
    /**
     * 启用日期：当月增加，下月开始折旧
     */
    private LocalDate startDate;
    /**
     * 原值
     */
    private BigDecimal originalValue;
    /**
     * 净残值率（百分数）
     */
    private BigDecimal residualRate;
    /**
     * 净残值
     */
    private BigDecimal residualValue;
    /**
     * 折旧月数
     */
    private Integer lifeMonths;
    /**
     * 进入系统前已折旧月数（旧资产录入用）
     */
    private Integer initDeprMonths;
    /**
     * 进入系统前累计折旧（旧资产录入用）
     */
    private BigDecimal initAccumDepr;
    /**
     * 状态：0 在用 1 已处置
     */
    private Integer status;
    /**
     * 处置日期
     */
    private LocalDate disposeDate;
    /**
     * 处置（转入清理）凭证
     */
    private Long disposeVoucherId;
    /**
     * 备注
     */
    private String remark;

}
