package cn.iocoder.yudao.module.fms.dal.dataobject.voucher;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * FMS 凭证分录 DO
 *
 * @author 芋道源码
 */
@TableName(value = "fms_voucher_entry", autoResultMap = true)
@KeySequence("fms_voucher_entry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsVoucherEntryDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 摘要内容
     */
    private String digest;
    /**
     * 科目名称快照
     *
     * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO#getName()}
     */
    private String subjectName;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 借方金额
     */
    private BigDecimal debitAmount;
    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;
    /**
     * 凭证编号
     *
     * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherDO#getId()}
     */
    private Long voucherId;
    /**
     * 科目编码快照
     *
     * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO#getCode()}
     */
    private String subjectCode;
    /**
     * 显示顺序
     */
    private Integer sort;
    /**
     * 科目编号
     *
     * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO#getId()}
     */
    private Long subjectId;
    /**
     * 账套编号
     *
     * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAccountSetDO#getId()}
     */
    private Long accountSetId;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 辅助核算组合编号
     *
     * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryCombinationDO#getId()}
     */
    private Long assistCombinationId;
    /**
     * 辅助核算项目数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<AuxiliaryItem> auxiliaries;
    /**
     * 出纳已勾对（库存现金、银行存款分录的单边勾对标记）
     */
    @TableField("cash_reconciled")
    private Boolean cashReconciled;

    /**
     * 凭证日期（联表查询时赋值，非分录表字段）
     */
    @TableField(exist = false)
    private java.time.LocalDateTime voucherTime;
    /**
     * 外币币别编号；为空表示本位币分录
     *
     * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsCurrencyDO#getId()}
     */
    private Long currencyId;
    /**
     * 汇率（外币分录记账汇率）
     */
    private BigDecimal exchangeRate;
    /**
     * 原币金额，正负号与本位币金额一致；期末调汇分录原币为 0
     */
    private BigDecimal originalAmount;

    /**
     * 辅助核算项目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuxiliaryItem {

        /**
         * 辅助核算类别
         *
         * 枚举 {@link cn.iocoder.yudao.module.fms.enums.config.FmsAuxiliaryTypeEnum}
         */
        private Integer type;
        /**
         * 辅助核算类别编号
         *
         * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryTypeDO#getId()}
         */
        private Long typeId;
        /**
         * 辅助核算项目编号
         *
         * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO#getId()}
         */
        private Long itemId;
        /**
         * 辅助核算项目名称快照
         *
         * 关联 {@link cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO#getName()}
         */
        private String name;

    }

}
