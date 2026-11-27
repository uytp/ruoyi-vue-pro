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
 * FMS 固定资产折旧明细 DO：一个资产一个月一条
 *
 * @author 芋道源码
 */
@TableName("fms_asset_depreciation")
@KeySequence("fms_asset_depreciation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsAssetDepreciationDO extends BaseDO {

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
     * 资产卡片编号
     */
    private Long assetId;
    /**
     * 会计期间 yyyy-MM
     */
    private String period;
    /**
     * 本月折旧额
     */
    private BigDecimal amount;
    /**
     * 计提后累计折旧
     */
    private BigDecimal accumAfter;
    /**
     * 计提后净值
     */
    private BigDecimal netAfter;
    /**
     * 折旧凭证
     */
    private Long voucherId;

}
