package cn.iocoder.yudao.module.fms.service.cost;

import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostReportRespVO;

/**
 * FMS 成本报表 Service：基于月度成本计算单，出产品成本汇总表和成本明细表
 */
public interface FmsCostReportService {

    /**
     * 获得成本报表
     *
     * @param accountSetId 账套编号
     * @param startPeriod  开始月份 yyyy-MM
     * @param endPeriod    结束月份 yyyy-MM
     * @param includeDraft 是否包含还没生成凭证的草稿成本单
     * @param userId       当前用户
     */
    FmsCostReportRespVO getCostReport(Long accountSetId, String startPeriod, String endPeriod,
            boolean includeDraft, Long userId);

}
