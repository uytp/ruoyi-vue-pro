package cn.iocoder.yudao.module.fms.service.cost;

import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSettingVO;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSheetCalcReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSheetRespVO;

/**
 * FMS 成本核算 Service（品种法，按月计算）
 *
 * 流程：成本参数 → 计算成本（从凭证归集、分配制造费用、划分完工与在产品、算单位成本和销售成本）
 * → 生成凭证（制造费用分配、完工入库、销售成本结转）→ 需要时撤销凭证后重新计算。
 */
public interface FmsCostService {

    /**
     * 获得成本参数；还没保存过时，按常用科目编码给出默认值
     */
    FmsCostSettingVO getCostSetting(Long accountSetId, Long userId);

    /**
     * 保存成本参数
     */
    void saveCostSetting(FmsCostSettingVO reqVO, Long userId);

    /**
     * 获得某月成本计算单；还没计算过时返回 id 为空的空单
     */
    FmsCostSheetRespVO getCostSheet(Long accountSetId, String period, Long userId);

    /**
     * 计算某月成本（可重复执行，已生成凭证后不能重算）
     */
    FmsCostSheetRespVO calculateCostSheet(FmsCostSheetCalcReqVO reqVO, Long userId);

    /**
     * 根据成本计算单生成凭证（制造费用分配、完工入库、销售成本结转）
     */
    FmsCostSheetRespVO generateCostVouchers(Long accountSetId, String period, Long userId);

    /**
     * 撤销已生成的成本凭证（凭证已审核时需先反审核）
     */
    FmsCostSheetRespVO revokeCostVouchers(Long accountSetId, String period, Long userId);

}
