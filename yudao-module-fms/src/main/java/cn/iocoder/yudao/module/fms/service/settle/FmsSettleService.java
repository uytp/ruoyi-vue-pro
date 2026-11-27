package cn.iocoder.yudao.module.fms.service.settle;

import cn.iocoder.yudao.module.fms.controller.admin.settle.vo.*;

import java.util.Collection;
import java.util.List;

/**
 * FMS 往来核销与账龄 Service 接口
 *
 * @author 芋道源码
 */
public interface FmsSettleService {

    /**
     * 获得可以做往来核销的科目（挂了客户 / 供应商 / 职员辅助核算的科目）
     */
    List<FmsSettleSubjectRespVO> getSettleSubjectList(Long accountSetId, Long userId);

    /**
     * 获得某个往来单位在某个科目下的借方、贷方未核销项目
     */
    FmsSettleOpenItemsRespVO getOpenItems(FmsSettleItemQueryReqVO reqVO, Long userId);

    /**
     * 核销：把选中的借方和贷方互相冲抵
     *
     * @return 核销批次号
     */
    Long createSettle(FmsSettleCreateReqVO reqVO, Long userId);

    /**
     * 自动核销：借贷未核销余额完全相等的一对一核销
     *
     * @return 核销了多少对
     */
    Integer autoSettle(FmsSettleAutoReqVO reqVO, Long userId);

    /**
     * 撤销一次核销
     */
    void revokeSettle(Long accountSetId, Long batchId, Long userId);

    /**
     * 获得某个往来单位的核销记录，一次核销一行
     */
    List<FmsSettleBatchRespVO> getSettleBatchList(FmsSettleItemQueryReqVO reqVO, Long userId);

    /**
     * 账龄分析
     */
    FmsSettleAgingRespVO getAging(FmsSettleAgingReqVO reqVO, Long userId);

    /**
     * 校验这些凭证分录没有被往来核销引用；被引用的凭证不能修改、删除、作废，要先撤销核销
     */
    void validateEntriesNotSettled(Long accountSetId, Collection<Long> entryIds);

}
