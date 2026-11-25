package cn.iocoder.yudao.module.fms.service.cash;

import cn.iocoder.yudao.module.fms.controller.admin.cash.vo.*;

import java.util.List;

/**
 * FMS 出纳与银行对账 Service
 *
 * @author 芋道源码
 */
public interface FmsCashBankService {

    /**
     * 获得出纳日记账：指定科目（库存现金 / 银行存款）在期间内的分录和余额
     */
    List<FmsCashJournalRespVO> getCashJournalList(FmsCashJournalReqVO reqVO, Long userId);

    /**
     * 勾对或取消勾对单条凭证分录（只影响出纳标记，不改凭证）
     */
    void updateCashReconciled(FmsCashReconcileReqVO reqVO, Long userId);

    /**
     * 导入银行对账单（标准 Excel），按流水号去重，返回导入条数
     */
    int importBankStatement(Long accountSetId, Long subjectId, List<FmsBankStatementImportVO> rows, Long userId);

    /**
     * 获得银行对账单列表
     */
    List<FmsBankStatementRespVO> getBankStatementList(FmsBankStatementQueryReqVO reqVO, Long userId);

    /**
     * 自动勾对：金额相等、日期相差 3 天内、一一对应的日记账与对账单勾对，返回匹配条数
     */
    int autoReconcile(Long accountSetId, Long subjectId, String month, Long userId);

    /**
     * 生成 / 重新计算银行余额调节表，返回调节结果
     */
    FmsBankReconcileRespVO calculateBankReconcile(Long accountSetId, Long subjectId, String month, Long userId);

}
