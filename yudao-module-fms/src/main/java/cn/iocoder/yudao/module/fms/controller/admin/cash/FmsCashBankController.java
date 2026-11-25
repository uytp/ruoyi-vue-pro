package cn.iocoder.yudao.module.fms.controller.admin.cash;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.fms.controller.admin.cash.vo.*;
import cn.iocoder.yudao.module.fms.service.cash.FmsCashBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - FMS 出纳与银行对账")
@RestController
@RequestMapping("/fms/cash")
@Validated
public class FmsCashBankController {

    @Resource
    private FmsCashBankService cashBankService;

    @GetMapping("/journal/list")
    @Operation(summary = "获得出纳日记账")
    @PreAuthorize("@ss.hasPermission('fms:cash:query')")
    public CommonResult<List<FmsCashJournalRespVO>> getCashJournalList(@Valid FmsCashJournalReqVO reqVO) {
        return success(cashBankService.getCashJournalList(reqVO, getLoginUserId()));
    }

    @PutMapping("/reconcile/update")
    @Operation(summary = "出纳勾对 / 取消勾对")
    @PreAuthorize("@ss.hasPermission('fms:cash:reconcile')")
    public CommonResult<Boolean> updateCashReconciled(@Valid @RequestBody FmsCashReconcileReqVO reqVO) {
        cashBankService.updateCashReconciled(reqVO, getLoginUserId());
        return success(true);
    }

    @PostMapping("/bank-statement/import")
    @Operation(summary = "导入银行对账单（标准 Excel）")
    @PreAuthorize("@ss.hasPermission('fms:cash:import')")
    public CommonResult<Integer> importBankStatement(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("file") MultipartFile file) throws IOException {
        List<FmsBankStatementImportVO> rows = FmsBankStatementImportExcelHelper.read(file);
        return success(cashBankService.importBankStatement(accountSetId, subjectId, rows, getLoginUserId()));
    }

    @GetMapping("/bank-statement/list")
    @Operation(summary = "获得银行对账单列表")
    @PreAuthorize("@ss.hasPermission('fms:cash:query')")
    public CommonResult<List<FmsBankStatementRespVO>> getBankStatementList(@Valid FmsBankStatementQueryReqVO reqVO) {
        return success(cashBankService.getBankStatementList(reqVO, getLoginUserId()));
    }

    @PostMapping("/reconcile/auto")
    @Operation(summary = "自动勾对（金额相等、日期差 3 天内）")
    @PreAuthorize("@ss.hasPermission('fms:cash:reconcile')")
    public CommonResult<Integer> autoReconcile(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("month") String month) {
        return success(cashBankService.autoReconcile(accountSetId, subjectId, month, getLoginUserId()));
    }

    @PostMapping("/bank-reconcile/calculate")
    @Operation(summary = "生成 / 重新计算银行余额调节表")
    @PreAuthorize("@ss.hasPermission('fms:cash:query')")
    public CommonResult<FmsBankReconcileRespVO> calculateBankReconcile(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("month") String month) {
        return success(cashBankService.calculateBankReconcile(accountSetId, subjectId, month, getLoginUserId()));
    }

}
