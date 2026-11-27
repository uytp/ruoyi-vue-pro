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
 * FMS 税务参数 DO：一个账套一条
 *
 * @author 芋道源码
 */
@TableName("fms_tax_setting")
@KeySequence("fms_tax_setting_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsTaxSettingDO extends BaseDO {

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
     * 开始用本系统算增值税的月份 yyyy-MM，空则取账套启用月份
     */
    private String startMonth;
    /**
     * 开始月份的上期留抵税额
     */
    private BigDecimal initCarry;
    /**
     * 城市维护建设税税率（百分数）
     */
    private BigDecimal urbanRate;
    /**
     * 教育费附加征收率（百分数）
     */
    private BigDecimal eduRate;
    /**
     * 地方教育费附加征收率（百分数）
     */
    private BigDecimal localEduRate;

}
