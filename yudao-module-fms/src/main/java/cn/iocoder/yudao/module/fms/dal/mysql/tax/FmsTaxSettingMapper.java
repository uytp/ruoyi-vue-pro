package cn.iocoder.yudao.module.fms.dal.mysql.tax;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.tax.FmsTaxSettingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 税务参数 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsTaxSettingMapper extends BaseMapperX<FmsTaxSettingDO> {

    /**
     * 取这个账套的税务参数；万一并发保存产生了多条，取编号最小的一条
     */
    default FmsTaxSettingDO selectByAccountSetId(Long accountSetId) {
        List<FmsTaxSettingDO> list = selectList(new LambdaQueryWrapperX<FmsTaxSettingDO>()
                .eq(FmsTaxSettingDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsTaxSettingDO::getId));
        return list.isEmpty() ? null : list.get(0);
    }

}
