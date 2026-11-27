package cn.iocoder.yudao.module.fms.service.asset;

import cn.iocoder.yudao.module.fms.controller.admin.asset.vo.*;

import java.util.List;

/**
 * FMS 固定资产 Service 接口
 *
 * @author 芋道源码
 */
public interface FmsAssetService {

    // ==================== 资产类别 ====================

    List<FmsAssetCategoryRespVO> getCategoryList(Long accountSetId, Long userId);

    /**
     * 新增或修改资产类别（id 为空是新增）
     */
    Long saveCategory(FmsAssetCategorySaveReqVO reqVO, Long userId);

    void deleteCategory(Long accountSetId, Long id, Long userId);

    // ==================== 资产卡片 ====================

    /**
     * 获得资产卡片列表
     *
     * @param categoryId 只看这个类别，可空
     * @param status 只看这个状态（0 在用 1 已处置），可空
     * @param keyword 编号或名称里包含的文字，可空
     */
    List<FmsAssetRespVO> getAssetList(Long accountSetId, Long categoryId, Integer status, String keyword, Long userId);

    /**
     * 新增或修改资产卡片（id 为空是新增）
     */
    Long saveAsset(FmsAssetSaveReqVO reqVO, Long userId);

    void deleteAsset(Long accountSetId, Long id, Long userId);

    /**
     * 处置资产：生成“转入固定资产清理”凭证，并把资产标记为已处置
     *
     * @return 凭证编号
     */
    Long disposeAsset(FmsAssetDisposeReqVO reqVO, Long userId);

    /**
     * 撤销处置：删除转入清理凭证，资产恢复为在用
     */
    void revokeDispose(Long accountSetId, Long id, Long userId);

    // ==================== 折旧 ====================

    /**
     * 获得某个月的折旧：已经计提的显示实际结果，还没计提的显示预览
     */
    FmsAssetDepreciationRespVO getDepreciation(Long accountSetId, String period, Long userId);

    /**
     * 计提折旧：生成一张折旧凭证
     *
     * @return 凭证编号
     */
    Long generateDepreciation(FmsAssetDepreciationReqVO reqVO, Long userId);

    /**
     * 撤销折旧：删除折旧凭证和明细，只能从最近计提的月份往前撤销
     */
    void revokeDepreciation(FmsAssetDepreciationReqVO reqVO, Long userId);

    // ==================== 对账 ====================

    /**
     * 固定资产卡片与总账对账
     */
    FmsAssetReconcileRespVO getReconcile(Long accountSetId, String month, Long userId);

}
