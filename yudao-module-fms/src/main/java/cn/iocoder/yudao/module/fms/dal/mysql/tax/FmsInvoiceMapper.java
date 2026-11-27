package cn.iocoder.yudao.module.fms.dal.mysql.tax;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.tax.FmsInvoiceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 发票台账 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsInvoiceMapper extends BaseMapperX<FmsInvoiceDO> {

    default List<FmsInvoiceDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsInvoiceDO>()
                .eq(FmsInvoiceDO::getAccountSetId, accountSetId)
                .orderByDesc(FmsInvoiceDO::getInvoiceDate)
                .orderByDesc(FmsInvoiceDO::getId));
    }

    default FmsInvoiceDO selectByUnique(Long accountSetId, Integer direction, String invoiceCode, String invoiceNo) {
        return selectOne(new LambdaQueryWrapperX<FmsInvoiceDO>()
                .eq(FmsInvoiceDO::getAccountSetId, accountSetId)
                .eq(FmsInvoiceDO::getDirection, direction)
                .eq(FmsInvoiceDO::getInvoiceCode, invoiceCode)
                .eq(FmsInvoiceDO::getInvoiceNo, invoiceNo));
    }

}
