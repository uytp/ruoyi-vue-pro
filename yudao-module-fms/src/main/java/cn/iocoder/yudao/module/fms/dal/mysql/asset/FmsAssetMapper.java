package cn.iocoder.yudao.module.fms.dal.mysql.asset;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.asset.FmsAssetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 固定资产卡片 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsAssetMapper extends BaseMapperX<FmsAssetDO> {

    default List<FmsAssetDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsAssetDO>()
                .eq(FmsAssetDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsAssetDO::getCode));
    }

    default FmsAssetDO selectByAccountSetIdAndCode(Long accountSetId, String code) {
        return selectOne(new LambdaQueryWrapperX<FmsAssetDO>()
                .eq(FmsAssetDO::getAccountSetId, accountSetId)
                .eq(FmsAssetDO::getCode, code));
    }

    default Long selectCountByCategoryId(Long accountSetId, Long categoryId) {
        return selectCount(new LambdaQueryWrapperX<FmsAssetDO>()
                .eq(FmsAssetDO::getAccountSetId, accountSetId)
                .eq(FmsAssetDO::getCategoryId, categoryId));
    }

}
