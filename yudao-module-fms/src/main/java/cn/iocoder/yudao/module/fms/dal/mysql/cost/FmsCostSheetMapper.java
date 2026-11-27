package cn.iocoder.yudao.module.fms.dal.mysql.cost;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSheetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FmsCostSheetMapper extends BaseMapperX<FmsCostSheetDO> {

    default FmsCostSheetDO selectByAccountSetIdAndPeriod(Long accountSetId, String period) {
        List<FmsCostSheetDO> list = selectList(new LambdaQueryWrapperX<FmsCostSheetDO>()
                .eq(FmsCostSheetDO::getAccountSetId, accountSetId)
                .eq(FmsCostSheetDO::getPeriod, period)
                .orderByAsc(FmsCostSheetDO::getId));
        return list.isEmpty() ? null : list.get(0);
    }

}
