package cn.iocoder.yudao.module.fms.dal.dataobject.tax;

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
 * FMS 发票台账 DO
 *
 * @author 芋道源码
 */
@TableName("fms_invoice")
@KeySequence("fms_invoice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmsInvoiceDO extends BaseDO {

    /** 方向：销项（开出去的） */
    public static final int DIRECTION_OUTPUT = 1;
    /** 方向：进项（收进来的） */
    public static final int DIRECTION_INPUT = 2;
    /** 种类：增值税专用发票 */
    public static final int TYPE_SPECIAL = 1;
    /** 种类：增值税普通发票 */
    public static final int TYPE_NORMAL = 2;
    /** 状态：正常 */
    public static final int STATUS_NORMAL = 0;
    /** 状态：作废 */
    public static final int STATUS_VOIDED = 1;

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
     * 方向：1 销项 2 进项
     */
    private Integer direction;
    /**
     * 发票种类：1 专票 2 普票
     */
    private Integer invoiceType;
    /**
     * 发票代码
     */
    private String invoiceCode;
    /**
     * 发票号码
     */
    private String invoiceNo;
    /**
     * 开票日期
     */
    private LocalDate invoiceDate;
    /**
     * 对方单位
     */
    private String counterpartName;
    /**
     * 对方税号
     */
    private String counterpartTaxNo;
    /**
     * 不含税金额，红字发票为负数
     */
    private BigDecimal amount;
    /**
     * 税率（百分数）
     */
    private BigDecimal taxRate;
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    /**
     * 价税合计
     */
    private BigDecimal totalAmount;
    /**
     * 状态：0 正常 1 作废
     */
    private Integer status;
    /**
     * 认证状态：0 未认证 1 已认证
     */
    private Integer certifyStatus;
    /**
     * 认证所属期 yyyy-MM
     */
    private String certifyPeriod;
    /**
     * 备注
     */
    private String remark;

}
