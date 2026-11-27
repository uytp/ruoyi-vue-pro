package cn.iocoder.yudao.module.fms.dal.mysql.asset;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.asset.FmsAssetCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 固定资产类别 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsAssetCategoryMapper extends BaseMapperX<FmsAssetCategoryDO> {

    default List<FmsAssetCategoryDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsAssetCategoryDO>()
                .eq(FmsAssetCategoryDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsAssetCategoryDO::getCode));
    }

    default FmsAssetCategoryDO selectByAccountSetIdAndCode(Long accountSetId, String code) {
        return selectOne(new LambdaQueryWrapperX<FmsAssetCategoryDO>()
                .eq(FmsAssetCategoryDO::getAccountSetId, accountSetId)
                .eq(FmsAssetCategoryDO::getCode, code));
    }

}
