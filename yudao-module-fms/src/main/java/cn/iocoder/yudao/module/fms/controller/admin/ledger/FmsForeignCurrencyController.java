package cn.iocoder.yudao.module.fms.controller.admin.ledger;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency.*;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsCurrencyRateDO;
import cn.iocoder.yudao.module.fms.service.ledger.FmsForeignCurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - FMS 外币核算")
@RestController
@RequestMapping("/fms/foreign-currency")
@Validated
public class FmsForeignCurrencyController {

    @Resource
    private FmsForeignCurrencyService foreignCurrencyService;

    // ========== 汇率表 ==========

    @GetMapping("/rate/list")
    @Operation(summary = "获得币别汇率表")
    @PreAuthorize("@ss.hasPermission('fms:config:currency:query')")
    public CommonResult<List<FmsCurrencyRateRespVO>> getRateList(@RequestParam("accountSetId") @NotNull Long accountSetId,
            @RequestParam(value = "currencyId", required = false) Long currencyId) {
        List<FmsCurrencyRateDO> list = foreignCurrencyService.getCurrencyRateList(accountSetId, currencyId, getLoginUserId());
        return success(BeanUtils.toBean(list, FmsCurrencyRateRespVO.class));
    }

    @GetMapping("/rate/month-list")
    @Operation(summary = "获得指定期间各外币的有效汇率（录凭证用）")
    public CommonResult<List<FmsCurrencyRateRespVO>> getMonthRateList(
            @RequestParam("accountSetId") @NotNull Long accountSetId, @RequestParam("month") String month) {
        return success(foreignCurrencyService.getMonthRateList(accountSetId, month, getLoginUserId()));
    }

    @PutMapping("/rate/save")
    @Operation(summary = "保存币别汇率")
    @PreAuthorize("@ss.hasPermission('fms:config:currency:update')")
    public CommonResult<Long> saveRate(@Valid @RequestBody FmsCurrencyRateSaveReqVO reqVO) {
        return success(foreignCurrencyService.saveCurrencyRate(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/rate/delete")
    @Operation(summary = "删除币别汇率")
    @PreAuthorize("@ss.hasPermission('fms:config:currency:update')")
    public CommonResult<Boolean> deleteRate(@RequestParam("accountSetId") @NotNull Long accountSetId,
            @RequestParam("id") @NotNull Long id) {
        foreignCurrencyService.deleteCurrencyRate(accountSetId, id, getLoginUserId());
        return success(true);
    }

    // ========== 外币期初 ==========

    @GetMapping("/initial-balance/list")
    @Operation(summary = "获得外币期初余额")
    @PreAuthorize("@ss.hasPermission('fms:config:initial-balance:query')")
    public CommonResult<List<FmsInitialCurrencyBalanceRespVO>> getInitialBalanceList(
            @RequestParam("accountSetId") @NotNull Long accountSetId) {
        return success(foreignCurrencyService.getInitialCurrencyBalanceList(accountSetId, getLoginUserId()));
    }

    @PutMapping("/initial-balance/save")
    @Operation(summary = "保存外币期初余额")
    @PreAuthorize("@ss.hasPermission('fms:config:initial-balance:update')")
    public CommonResult<Boolean> saveInitialBalance(@Valid @RequestBody FmsInitialCurrencyBalanceSaveReqVO reqVO) {
        foreignCurrencyService.saveInitialCurrencyBalance(reqVO, getLoginUserId());
        return success(true);
    }

    // ========== 外币账簿 ==========

    @GetMapping("/ledger/balance/list")
    @Operation(summary = "获得外币余额表")
    @PreAuthorize("@ss.hasPermission('fms:ledger:currency:query')")
    public CommonResult<List<FmsLedgerCurrencyBalanceRespVO>> getBalanceList(@Valid FmsLedgerCurrencyReqVO reqVO) {
        return success(foreignCurrencyService.getCurrencyBalanceList(reqVO, getLoginUserId()));
    }

    @GetMapping("/ledger/detail/list")
    @Operation(summary = "获得外币明细账")
    @PreAuthorize("@ss.hasPermission('fms:ledger:currency:query')")
    public CommonResult<List<FmsLedgerCurrencyDetailRespVO>> getDetailList(@Valid FmsLedgerCurrencyReqVO reqVO) {
        return success(foreignCurrencyService.getCurrencyDetailList(reqVO, getLoginUserId()));
    }

    // ========== 期末调汇 ==========

    @PostMapping("/exchange-adjust/preview")
    @Operation(summary = "期末调汇预览")
    @PreAuthorize("@ss.hasPermission('fms:closing:exchange-adjust')")
    public CommonResult<FmsExchangeAdjustRespVO> previewExchangeAdjust(@Valid @RequestBody FmsExchangeAdjustReqVO reqVO) {
        return success(foreignCurrencyService.previewExchangeAdjust(reqVO, getLoginUserId()));
    }

    @PostMapping("/exchange-adjust/generate")
    @Operation(summary = "生成期末调汇凭证")
    @PreAuthorize("@ss.hasPermission('fms:closing:exchange-adjust')")
    public CommonResult<FmsExchangeAdjustRespVO> generateExchangeAdjust(@Valid @RequestBody FmsExchangeAdjustReqVO reqVO) {
        return success(foreignCurrencyService.generateExchangeAdjust(reqVO, getLoginUserId()));
    }

}
