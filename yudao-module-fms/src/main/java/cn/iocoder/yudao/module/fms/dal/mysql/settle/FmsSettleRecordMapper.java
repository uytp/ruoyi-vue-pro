package cn.iocoder.yudao.module.fms.dal.mysql.settle;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.settle.FmsSettleRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Collection;

/**
 * FMS 往来核销记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsSettleRecordMapper extends BaseMapperX<FmsSettleRecordDO> {

    /**
     * 这些来源标识（借方或贷方）被核销记录引用了多少次
     */
    default Long selectCountByRefs(Long accountSetId, Collection<String> refs) {
        return selectCount(new LambdaQueryWrapperX<FmsSettleRecordDO>()
                .eq(FmsSettleRecordDO::getAccountSetId, accountSetId)
                .and(wrapper -> wrapper.in(FmsSettleRecordDO::getDebitRef, refs)
                        .or().in(FmsSettleRecordDO::getCreditRef, refs)));
    }

    /**
     * 查询某个往来对象（某个客户 / 供应商 / 职员）的全部核销记录
     */
    default List<FmsSettleRecordDO> selectListByItem(Long accountSetId, Long auxiliaryTypeId, Long auxiliaryItemId) {
        return selectList(new LambdaQueryWrapperX<FmsSettleRecordDO>()
                .eq(FmsSettleRecordDO::getAccountSetId, accountSetId)
                .eq(FmsSettleRecordDO::getAuxiliaryTypeId, auxiliaryTypeId)
                .eqIfPresent(FmsSettleRecordDO::getAuxiliaryItemId, auxiliaryItemId)
                .orderByAsc(FmsSettleRecordDO::getId));
    }

    default List<FmsSettleRecordDO> selectListByBatchId(Long accountSetId, Long batchId) {
        return selectList(new LambdaQueryWrapperX<FmsSettleRecordDO>()
                .eq(FmsSettleRecordDO::getAccountSetId, accountSetId)
                .eq(FmsSettleRecordDO::getBatchId, batchId));
    }

    default int deleteByBatchId(Long accountSetId, Long batchId) {
        return delete(new LambdaQueryWrapperX<FmsSettleRecordDO>()
                .eq(FmsSettleRecordDO::getAccountSetId, accountSetId)
                .eq(FmsSettleRecordDO::getBatchId, batchId));
    }

}
