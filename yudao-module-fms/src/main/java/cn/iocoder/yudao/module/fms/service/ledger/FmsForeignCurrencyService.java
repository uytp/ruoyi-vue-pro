package cn.iocoder.yudao.module.fms.service.ledger;

import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency.*;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsCurrencyRateDO;

import java.util.List;

/**
 * FMS 外币核算 Service：汇率表、外币期初、外币账簿、期末调汇
 */
public interface FmsForeignCurrencyService {

    List<FmsCurrencyRateDO> getCurrencyRateList(Long accountSetId, Long currencyId, Long userId);

    Long saveCurrencyRate(FmsCurrencyRateSaveReqVO reqVO, Long userId);

    void deleteCurrencyRate(Long accountSetId, Long id, Long userId);

    /**
     * 获得指定期间各外币的有效汇率：本期汇率表 → 以前期间最近一条 → 币别默认汇率
     */
    List<FmsCurrencyRateRespVO> getMonthRateList(Long accountSetId, String month, Long userId);

    List<FmsInitialCurrencyBalanceRespVO> getInitialCurrencyBalanceList(Long accountSetId, Long userId);

    void saveInitialCurrencyBalance(FmsInitialCurrencyBalanceSaveReqVO reqVO, Long userId);

    List<FmsLedgerCurrencyBalanceRespVO> getCurrencyBalanceList(FmsLedgerCurrencyReqVO reqVO, Long userId);

    List<FmsLedgerCurrencyDetailRespVO> getCurrencyDetailList(FmsLedgerCurrencyReqVO reqVO, Long userId);

    FmsExchangeAdjustRespVO previewExchangeAdjust(FmsExchangeAdjustReqVO reqVO, Long userId);

    FmsExchangeAdjustRespVO generateExchangeAdjust(FmsExchangeAdjustReqVO reqVO, Long userId);

}
