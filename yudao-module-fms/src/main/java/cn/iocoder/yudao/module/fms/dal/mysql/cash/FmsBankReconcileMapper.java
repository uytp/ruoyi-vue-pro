package cn.iocoder.yudao.module.fms.dal.mysql.cash;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.cash.FmsBankReconcileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 银行余额调节表 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsBankReconcileMapper extends BaseMapperX<FmsBankReconcileDO> {

    default FmsBankReconcileDO selectByAccountSetIdAndSubjectIdAndMonth(Long accountSetId, Long subjectId,
            String month) {
        return selectOne(new LambdaQueryWrapperX<FmsBankReconcileDO>()
                .eq(FmsBankReconcileDO::getAccountSetId, accountSetId)
                .eq(FmsBankReconcileDO::getSubjectId, subjectId)
                .eq(FmsBankReconcileDO::getMonth, month));
    }

    default List<FmsBankReconcileDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsBankReconcileDO>()
                .eq(FmsBankReconcileDO::getAccountSetId, accountSetId)
                .orderByDesc(FmsBankReconcileDO::getMonth));
    }

}
