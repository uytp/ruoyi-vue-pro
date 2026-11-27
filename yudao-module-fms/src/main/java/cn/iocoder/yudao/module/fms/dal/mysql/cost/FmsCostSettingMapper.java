package cn.iocoder.yudao.module.fms.dal.mysql.cost;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSettingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FmsCostSettingMapper extends BaseMapperX<FmsCostSettingDO> {

    /** 取账套的成本参数；极端情况下有重复时只取 id 最小的一条 */
    default FmsCostSettingDO selectByAccountSetId(Long accountSetId) {
        List<FmsCostSettingDO> list = selectList(new LambdaQueryWrapperX<FmsCostSettingDO>()
                .eq(FmsCostSettingDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsCostSettingDO::getId));
        return list.isEmpty() ? null : list.get(0);
    }

}
