package cn.iocoder.yudao.module.fms.controller.admin.settle;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.settle.vo.*;
import cn.iocoder.yudao.module.fms.service.settle.FmsSettleService;
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

@Tag(name = "管理后台 - FMS 往来核销与账龄")
@RestController
@RequestMapping("/fms/settle")
@Validated
public class FmsSettleController {

    @Resource
    private FmsSettleService settleService;

    @GetMapping("/subject/list")
    @Operation(summary = "获得可做往来核销的科目")
    @PreAuthorize("@ss.hasPermission('fms:settle:query')")
    public CommonResult<List<FmsSettleSubjectRespVO>> getSettleSubjectList(
            @RequestParam("accountSetId") Long accountSetId) {
        return success(settleService.getSettleSubjectList(accountSetId, getLoginUserId()));
    }

    @GetMapping("/open-item/list")
    @Operation(summary = "获得往来单位的借方、贷方未核销项目")
    @PreAuthorize("@ss.hasPermission('fms:settle:query')")
    public CommonResult<FmsSettleOpenItemsRespVO> getOpenItems(@Valid FmsSettleItemQueryReqVO reqVO) {
        return success(settleService.getOpenItems(reqVO, getLoginUserId()));
    }

    @PostMapping("/create")
    @Operation(summary = "往来核销")
    @PreAuthorize("@ss.hasPermission('fms:settle:verify')")
    public CommonResult<Long> createSettle(@Valid @RequestBody FmsSettleCreateReqVO reqVO) {
        return success(settleService.createSettle(reqVO, getLoginUserId()));
    }

    @PostMapping("/auto")
    @Operation(summary = "自动核销（借贷未核销余额完全相等的一对一核销）")
    @PreAuthorize("@ss.hasPermission('fms:settle:auto')")
    public CommonResult<Integer> autoSettle(@Valid @RequestBody FmsSettleAutoReqVO reqVO) {
        return success(settleService.autoSettle(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/revoke")
    @Operation(summary = "撤销核销")
    @PreAuthorize("@ss.hasPermission('fms:settle:revoke')")
    public CommonResult<Boolean> revokeSettle(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("batchId") Long batchId) {
        settleService.revokeSettle(accountSetId, batchId, getLoginUserId());
        return success(true);
    }

    @GetMapping("/batch/list")
    @Operation(summary = "获得往来单位的核销记录")
    @PreAuthorize("@ss.hasPermission('fms:settle:query')")
    public CommonResult<List<FmsSettleBatchRespVO>> getSettleBatchList(@Valid FmsSettleItemQueryReqVO reqVO) {
        return success(settleService.getSettleBatchList(reqVO, getLoginUserId()));
    }

    @GetMapping("/aging/list")
    @Operation(summary = "账龄分析")
    @PreAuthorize("@ss.hasPermission('fms:settle:query')")
    public CommonResult<FmsSettleAgingRespVO> getAging(@Valid FmsSettleAgingReqVO reqVO) {
        return success(settleService.getAging(reqVO, getLoginUserId()));
    }

}
