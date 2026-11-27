package cn.iocoder.yudao.module.fms.dal.mysql.asset;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.asset.FmsAssetDepreciationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 固定资产折旧明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsAssetDepreciationMapper extends BaseMapperX<FmsAssetDepreciationDO> {

    default List<FmsAssetDepreciationDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsAssetDepreciationDO>()
                .eq(FmsAssetDepreciationDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsAssetDepreciationDO::getPeriod)
                .orderByAsc(FmsAssetDepreciationDO::getId));
    }

    default List<FmsAssetDepreciationDO> selectListByPeriod(Long accountSetId, String period) {
        return selectList(new LambdaQueryWrapperX<FmsAssetDepreciationDO>()
                .eq(FmsAssetDepreciationDO::getAccountSetId, accountSetId)
                .eq(FmsAssetDepreciationDO::getPeriod, period)
                .orderByAsc(FmsAssetDepreciationDO::getAssetId));
    }

    default List<FmsAssetDepreciationDO> selectListByAssetId(Long accountSetId, Long assetId) {
        return selectList(new LambdaQueryWrapperX<FmsAssetDepreciationDO>()
                .eq(FmsAssetDepreciationDO::getAccountSetId, accountSetId)
                .eq(FmsAssetDepreciationDO::getAssetId, assetId)
                .orderByAsc(FmsAssetDepreciationDO::getPeriod));
    }

    default int deleteByPeriod(Long accountSetId, String period) {
        return delete(new LambdaQueryWrapperX<FmsAssetDepreciationDO>()
                .eq(FmsAssetDepreciationDO::getAccountSetId, accountSetId)
                .eq(FmsAssetDepreciationDO::getPeriod, period));
    }

    default int deleteByVoucherId(Long accountSetId, Long voucherId) {
        return delete(new LambdaQueryWrapperX<FmsAssetDepreciationDO>()
                .eq(FmsAssetDepreciationDO::getAccountSetId, accountSetId)
                .eq(FmsAssetDepreciationDO::getVoucherId, voucherId));
    }

}
