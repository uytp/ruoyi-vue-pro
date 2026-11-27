package cn.iocoder.yudao.module.fms.dal.mysql.cost;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSheetItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FmsCostSheetItemMapper extends BaseMapperX<FmsCostSheetItemDO> {

    default List<FmsCostSheetItemDO> selectListBySheetId(Long sheetId) {
        return selectList(new LambdaQueryWrapperX<FmsCostSheetItemDO>()
                .eq(FmsCostSheetItemDO::getSheetId, sheetId)
                .orderByAsc(FmsCostSheetItemDO::getProductCode)
                .orderByAsc(FmsCostSheetItemDO::getId));
    }

    default void deleteBySheetId(Long sheetId) {
        delete(new LambdaQueryWrapperX<FmsCostSheetItemDO>().eq(FmsCostSheetItemDO::getSheetId, sheetId));
    }

}
