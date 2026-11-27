package cn.iocoder.yudao.module.fms.dal.mysql.tax;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.tax.FmsTaxRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 税务凭证记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsTaxRecordMapper extends BaseMapperX<FmsTaxRecordDO> {

    default List<FmsTaxRecordDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsTaxRecordDO>()
                .eq(FmsTaxRecordDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsTaxRecordDO::getPeriod)
                .orderByAsc(FmsTaxRecordDO::getId));
    }

}
