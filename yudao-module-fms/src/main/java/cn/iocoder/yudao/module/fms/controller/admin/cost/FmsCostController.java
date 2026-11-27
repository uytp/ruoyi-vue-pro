package cn.iocoder.yudao.module.fms.controller.admin.cost;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSettingVO;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSheetCalcReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSheetRespVO;
import cn.iocoder.yudao.module.fms.service.cost.FmsCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - FMS 成本核算")
@RestController
@RequestMapping("/fms/cost")
@Validated
public class FmsCostController {

    @Resource
    private FmsCostService costService;

    @GetMapping("/setting/get")
    @Operation(summary = "获得成本参数（没设置过时返回按常用科目编码推荐的默认值）")
    @PreAuthorize("@ss.hasPermission('fms:cost:query')")
    public CommonResult<FmsCostSettingVO> getCostSetting(@RequestParam("accountSetId") Long accountSetId) {
        return success(costService.getCostSetting(accountSetId, getLoginUserId()));
    }

    @PutMapping("/setting/save")
    @Operation(summary = "保存成本参数")
    @PreAuthorize("@ss.hasPermission('fms:cost:setting')")
    public CommonResult<Boolean> saveCostSetting(@Valid @RequestBody FmsCostSettingVO reqVO) {
        costService.saveCostSetting(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/sheet/get")
    @Operation(summary = "获得某月成本计算单")
    @PreAuthorize("@ss.hasPermission('fms:cost:query')")
    public CommonResult<FmsCostSheetRespVO> getCostSheet(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("period") String period) {
        return success(costService.getCostSheet(accountSetId, period, getLoginUserId()));
    }

    @PostMapping("/sheet/calculate")
    @Operation(summary = "计算某月成本")
    @PreAuthorize("@ss.hasPermission('fms:cost:calculate')")
    public CommonResult<FmsCostSheetRespVO> calculateCostSheet(@Valid @RequestBody FmsCostSheetCalcReqVO reqVO) {
        return success(costService.calculateCostSheet(reqVO, getLoginUserId()));
    }

    @PostMapping("/sheet/generate-vouchers")
    @Operation(summary = "根据成本计算单生成凭证")
    @PreAuthorize("@ss.hasPermission('fms:cost:generate')")
    public CommonResult<FmsCostSheetRespVO> generateCostVouchers(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("period") String period) {
        return success(costService.generateCostVouchers(accountSetId, period, getLoginUserId()));
    }

    @PostMapping("/sheet/revoke-vouchers")
    @Operation(summary = "撤销已生成的成本凭证")
    @PreAuthorize("@ss.hasPermission('fms:cost:generate')")
    public CommonResult<FmsCostSheetRespVO> revokeCostVouchers(@RequestParam("accountSetId") Long accountSetId,
            @RequestParam("period") String period) {
        return success(costService.revokeCostVouchers(accountSetId, period, getLoginUserId()));
    }

}
