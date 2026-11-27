package cn.iocoder.yudao.module.fms.controller.admin.tax;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.tax.vo.*;
import cn.iocoder.yudao.module.fms.service.tax.FmsTaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - FMS 发票与增值税")
@RestController
@RequestMapping("/fms/tax")
@Validated
public class FmsTaxController {

    @Resource
    private FmsTaxService taxService;

    // ==================== 发票台账 ====================

    @GetMapping("/invoice/list")
    @Operation(summary = "获得发票列表")
    @PreAuthorize("@ss.hasPermission('fms:invoice:query')")
    public CommonResult<List<FmsInvoiceRespVO>> getInvoiceList(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam(value = "direction", required = false) Integer direction,
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(taxService.getInvoiceList(accountSetId, direction, month, status, keyword, getLoginUserId()));
    }

    @PostMapping("/invoice/save")
    @Operation(summary = "新增或修改发票")
    @PreAuthorize("@ss.hasPermission('fms:invoice:save')")
    public CommonResult<Long> saveInvoice(@Valid @RequestBody FmsInvoiceSaveReqVO reqVO) {
        return success(taxService.saveInvoice(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/invoice/delete")
    @Operation(summary = "删除发票")
    @PreAuthorize("@ss.hasPermission('fms:invoice:delete')")
    public CommonResult<Boolean> deleteInvoice(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("id") Long id) {
        taxService.deleteInvoice(accountSetId, id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/invoice/void")
    @Operation(summary = "作废或恢复发票")
    @PreAuthorize("@ss.hasPermission('fms:invoice:save')")
    public CommonResult<Boolean> voidInvoices(@Valid @RequestBody FmsInvoiceBatchReqVO reqVO) {
        taxService.voidInvoices(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/invoice/certify")
    @Operation(summary = "认证或取消认证进项专票")
    @PreAuthorize("@ss.hasPermission('fms:invoice:certify')")
    public CommonResult<Boolean> certifyInvoices(@Valid @RequestBody FmsInvoiceBatchReqVO reqVO) {
        taxService.certifyInvoices(reqVO, getLoginUserId());
        return success(true);
    }

    // ==================== 税务参数 ====================

    @GetMapping("/setting/get")
    @Operation(summary = "获得税务参数")
    @PreAuthorize("@ss.hasPermission('fms:tax:query')")
    public CommonResult<FmsTaxSettingVO> getSetting(@RequestParam("accountSetId") Long accountSetId) {
        return success(taxService.getSetting(accountSetId, getLoginUserId()));
    }

    @PostMapping("/setting/save")
    @Operation(summary = "保存税务参数")
    @PreAuthorize("@ss.hasPermission('fms:tax:setting')")
    public CommonResult<Boolean> saveSetting(@Valid @RequestBody FmsTaxSettingVO reqVO) {
        taxService.saveSetting(reqVO, getLoginUserId());
        return success(true);
    }

    // ==================== 增值税申报 ====================

    @GetMapping("/vat/get")
    @Operation(summary = "获得某个月的增值税申报汇总")
    @PreAuthorize("@ss.hasPermission('fms:tax:query')")
    public CommonResult<FmsTaxVatRespVO> getVat(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("period") String period) {
        return success(taxService.getVat(accountSetId, period, getLoginUserId()));
    }

    @PostMapping("/voucher/generate")
    @Operation(summary = "生成税务结转凭证（未交增值税 + 附加税）")
    @PreAuthorize("@ss.hasPermission('fms:tax:generate')")
    public CommonResult<Long> generateTaxVoucher(@Valid @RequestBody FmsTaxPeriodReqVO reqVO) {
        return success(taxService.generateTaxVoucher(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/voucher/revoke")
    @Operation(summary = "撤销税务结转凭证")
    @PreAuthorize("@ss.hasPermission('fms:tax:revoke')")
    public CommonResult<Boolean> revokeTaxVoucher(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("period") String period) {
        FmsTaxPeriodReqVO reqVO = new FmsTaxPeriodReqVO();
        reqVO.setAccountSetId(accountSetId);
        reqVO.setPeriod(period);
        taxService.revokeTaxVoucher(reqVO, getLoginUserId());
        return success(true);
    }

}
