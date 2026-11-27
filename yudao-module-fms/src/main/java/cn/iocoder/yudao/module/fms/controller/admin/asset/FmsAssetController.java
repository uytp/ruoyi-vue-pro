package cn.iocoder.yudao.module.fms.controller.admin.asset;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.asset.vo.*;
import cn.iocoder.yudao.module.fms.service.asset.FmsAssetService;
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

@Tag(name = "管理后台 - FMS 固定资产")
@RestController
@RequestMapping("/fms/asset")
@Validated
public class FmsAssetController {

    @Resource
    private FmsAssetService assetService;

    // ==================== 资产类别 ====================

    @GetMapping("/category/list")
    @Operation(summary = "获得资产类别列表")
    @PreAuthorize("@ss.hasPermission('fms:asset:query')")
    public CommonResult<List<FmsAssetCategoryRespVO>> getCategoryList(@RequestParam("accountSetId") Long accountSetId) {
        return success(assetService.getCategoryList(accountSetId, getLoginUserId()));
    }

    @PostMapping("/category/save")
    @Operation(summary = "新增或修改资产类别")
    @PreAuthorize("@ss.hasPermission('fms:asset:category')")
    public CommonResult<Long> saveCategory(@Valid @RequestBody FmsAssetCategorySaveReqVO reqVO) {
        return success(assetService.saveCategory(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/category/delete")
    @Operation(summary = "删除资产类别")
    @PreAuthorize("@ss.hasPermission('fms:asset:category')")
    public CommonResult<Boolean> deleteCategory(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("id") Long id) {
        assetService.deleteCategory(accountSetId, id, getLoginUserId());
        return success(true);
    }

    // ==================== 资产卡片 ====================

    @GetMapping("/list")
    @Operation(summary = "获得资产卡片列表")
    @PreAuthorize("@ss.hasPermission('fms:asset:query')")
    public CommonResult<List<FmsAssetRespVO>> getAssetList(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(assetService.getAssetList(accountSetId, categoryId, status, keyword, getLoginUserId()));
    }

    @PostMapping("/save")
    @Operation(summary = "新增或修改资产卡片")
    @PreAuthorize("@ss.hasPermission('fms:asset:save')")
    public CommonResult<Long> saveAsset(@Valid @RequestBody FmsAssetSaveReqVO reqVO) {
        return success(assetService.saveAsset(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产卡片")
    @PreAuthorize("@ss.hasPermission('fms:asset:delete')")
    public CommonResult<Boolean> deleteAsset(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("id") Long id) {
        assetService.deleteAsset(accountSetId, id, getLoginUserId());
        return success(true);
    }

    @PostMapping("/dispose")
    @Operation(summary = "处置资产（生成转入固定资产清理凭证）")
    @PreAuthorize("@ss.hasPermission('fms:asset:dispose')")
    public CommonResult<Long> disposeAsset(@Valid @RequestBody FmsAssetDisposeReqVO reqVO) {
        return success(assetService.disposeAsset(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/dispose/revoke")
    @Operation(summary = "撤销处置")
    @PreAuthorize("@ss.hasPermission('fms:asset:dispose')")
    public CommonResult<Boolean> revokeDispose(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("id") Long id) {
        assetService.revokeDispose(accountSetId, id, getLoginUserId());
        return success(true);
    }

    // ==================== 折旧 ====================

    @GetMapping("/depreciation/get")
    @Operation(summary = "获得某个月的折旧（已计提显示实际，未计提显示预览）")
    @PreAuthorize("@ss.hasPermission('fms:asset:query')")
    public CommonResult<FmsAssetDepreciationRespVO> getDepreciation(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("period") String period) {
        return success(assetService.getDepreciation(accountSetId, period, getLoginUserId()));
    }

    @PostMapping("/depreciation/generate")
    @Operation(summary = "计提折旧并生成凭证")
    @PreAuthorize("@ss.hasPermission('fms:asset:depreciate')")
    public CommonResult<Long> generateDepreciation(@Valid @RequestBody FmsAssetDepreciationReqVO reqVO) {
        return success(assetService.generateDepreciation(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/depreciation/revoke")
    @Operation(summary = "撤销某个月的折旧")
    @PreAuthorize("@ss.hasPermission('fms:asset:revoke')")
    public CommonResult<Boolean> revokeDepreciation(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("period") String period) {
        FmsAssetDepreciationReqVO reqVO = new FmsAssetDepreciationReqVO();
        reqVO.setAccountSetId(accountSetId);
        reqVO.setPeriod(period);
        assetService.revokeDepreciation(reqVO, getLoginUserId());
        return success(true);
    }

    // ==================== 对账 ====================

    @GetMapping("/reconcile")
    @Operation(summary = "固定资产卡片与总账对账")
    @PreAuthorize("@ss.hasPermission('fms:asset:query')")
    public CommonResult<FmsAssetReconcileRespVO> getReconcile(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("month") String month) {
        return success(assetService.getReconcile(accountSetId, month, getLoginUserId()));
    }

}
