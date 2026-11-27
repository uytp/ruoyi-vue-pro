package cn.iocoder.yudao.module.fms.service.tax;

import cn.iocoder.yudao.module.fms.controller.admin.tax.vo.*;

import java.util.List;

/**
 * FMS 发票台账与增值税 Service 接口
 *
 * @author 芋道源码
 */
public interface FmsTaxService {

    // ==================== 发票台账 ====================

    /**
     * 获得发票列表
     *
     * @param direction 方向，可空
     * @param month 开票月份 yyyy-MM，可空
     * @param status 状态，可空
     * @param keyword 号码、对方单位里包含的文字，可空
     */
    List<FmsInvoiceRespVO> getInvoiceList(Long accountSetId, Integer direction, String month, Integer status,
            String keyword, Long userId);

    /**
     * 新增或修改发票（id 为空是新增）
     */
    Long saveInvoice(FmsInvoiceSaveReqVO reqVO, Long userId);

    void deleteInvoice(Long accountSetId, Long id, Long userId);

    /**
     * 作废（flag=true）或恢复（flag=false）
     */
    void voidInvoices(FmsInvoiceBatchReqVO reqVO, Long userId);

    /**
     * 认证（flag=true）或取消认证（flag=false），只对进项专票
     */
    void certifyInvoices(FmsInvoiceBatchReqVO reqVO, Long userId);

    // ==================== 税务参数 ====================

    FmsTaxSettingVO getSetting(Long accountSetId, Long userId);

    void saveSetting(FmsTaxSettingVO reqVO, Long userId);

    // ==================== 增值税申报 ====================

    /**
     * 增值税申报汇总：已生成凭证的月份也按当前发票重新算，但页面上以“是否已生成”为准
     */
    FmsTaxVatRespVO getVat(Long accountSetId, String period, Long userId);

    /**
     * 生成税务结转凭证：结转未交增值税 + 计提附加税，一张凭证
     *
     * @return 凭证编号
     */
    Long generateTaxVoucher(FmsTaxPeriodReqVO reqVO, Long userId);

    /**
     * 撤销税务凭证
     */
    void revokeTaxVoucher(FmsTaxPeriodReqVO reqVO, Long userId);

}
