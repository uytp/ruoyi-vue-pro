package cn.iocoder.yudao.module.fms.dal.mysql.cash;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.cash.FmsBankStatementDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * FMS 银行对账单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsBankStatementMapper extends BaseMapperX<FmsBankStatementDO> {

    default List<FmsBankStatementDO> selectListByAccountSetIdAndSubjectId(Long accountSetId, Long subjectId,
            LocalDate beginDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<FmsBankStatementDO>()
                .eq(FmsBankStatementDO::getAccountSetId, accountSetId)
                .eq(FmsBankStatementDO::getSubjectId, subjectId)
                .geIfPresent(FmsBankStatementDO::getStatementDate, beginDate)
                .leIfPresent(FmsBankStatementDO::getStatementDate, endDate)
                .orderByAsc(FmsBankStatementDO::getStatementDate)
                .orderByAsc(FmsBankStatementDO::getId));
    }

    default List<FmsBankStatementDO> selectListByReferenceNos(Long accountSetId, List<String> referenceNos) {
        return selectList(new LambdaQueryWrapperX<FmsBankStatementDO>()
                .eq(FmsBankStatementDO::getAccountSetId, accountSetId)
                .in(FmsBankStatementDO::getReferenceNo, referenceNos));
    }

}
