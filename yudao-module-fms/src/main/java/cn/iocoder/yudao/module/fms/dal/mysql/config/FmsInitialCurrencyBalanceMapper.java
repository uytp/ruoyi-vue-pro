package cn.iocoder.yudao.module.fms.dal.mysql.config;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsInitialCurrencyBalanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FmsInitialCurrencyBalanceMapper extends BaseMapperX<FmsInitialCurrencyBalanceDO> {

    default List<FmsInitialCurrencyBalanceDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(FmsInitialCurrencyBalanceDO::getAccountSetId, accountSetId);
    }

}
