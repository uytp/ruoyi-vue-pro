package cn.iocoder.yudao.module.fms.dal.mysql.config;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsCurrencyRateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FmsCurrencyRateMapper extends BaseMapperX<FmsCurrencyRateDO> {

    default List<FmsCurrencyRateDO> selectListByAccountSetId(Long accountSetId, Long currencyId) {
        return selectList(new LambdaQueryWrapperX<FmsCurrencyRateDO>()
                .eq(FmsCurrencyRateDO::getAccountSetId, accountSetId)
                .eqIfPresent(FmsCurrencyRateDO::getCurrencyId, currencyId)
                .orderByDesc(FmsCurrencyRateDO::getMonth)
                .orderByDesc(FmsCurrencyRateDO::getId));
    }

    default FmsCurrencyRateDO selectByMonth(Long accountSetId, Long currencyId, String month) {
        return selectOne(FmsCurrencyRateDO::getAccountSetId, accountSetId,
                FmsCurrencyRateDO::getCurrencyId, currencyId, FmsCurrencyRateDO::getMonth, month);
    }

    default FmsCurrencyRateDO selectByIdAndAccountSetId(Long id, Long accountSetId) {
        return selectOne(FmsCurrencyRateDO::getId, id, FmsCurrencyRateDO::getAccountSetId, accountSetId);
    }

}
