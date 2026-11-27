package cn.iocoder.yudao.module.fms.dal.mysql.cost;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    /** 查询某段月份内的成本单（period 是 yyyy-MM，字符串比较即按时间先后） */
    default List<FmsCostSheetDO> selectListByPeriodRange(Long accountSetId, String startPeriod, String endPeriod,
            boolean onlyGenerated) {
        LambdaQueryWrapper<FmsCostSheetDO> wrapper = new LambdaQueryWrapperX<FmsCostSheetDO>()
                .eq(FmsCostSheetDO::getAccountSetId, accountSetId)
                .ge(FmsCostSheetDO::getPeriod, startPeriod)
                .le(FmsCostSheetDO::getPeriod, endPeriod);
        if (onlyGenerated) {
            wrapper.eq(FmsCostSheetDO::getStatus, FmsCostSheetDO.STATUS_GENERATED);
        }
        wrapper.orderByAsc(FmsCostSheetDO::getPeriod);
        return selectList(wrapper);
    }

}
