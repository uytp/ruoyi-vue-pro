package cn.iocoder.yudao.module.fms.service.ledger;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.fms.controller.admin.config.vo.initialbalance.FmsInitialBalanceSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.FmsLedgerEntryVO;
import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.currency.*;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherEntrySaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.*;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsCurrencyMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsCurrencyRateMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsInitialBalanceMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsInitialCurrencyBalanceMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.ledger.FmsLedgerQueryMapper;
import cn.iocoder.yudao.module.fms.enums.common.FmsDebitCreditDirectionEnum;
import cn.iocoder.yudao.module.fms.service.closing.FmsClosingPeriodService;
import cn.iocoder.yudao.module.fms.service.config.FmsAccountSetService;
import cn.iocoder.yudao.module.fms.service.config.FmsInitialBalanceService;
import cn.iocoder.yudao.module.fms.service.config.FmsSubjectService;
import cn.iocoder.yudao.module.fms.service.config.FmsVoucherWordService;
import cn.iocoder.yudao.module.fms.service.voucher.FmsVoucherService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.filterList;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;

@Service
@Validated
public class FmsForeignCurrencyServiceImpl implements FmsForeignCurrencyService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private FmsCurrencyMapper currencyMapper;
    @Resource
    private FmsCurrencyRateMapper currencyRateMapper;
    @Resource
    private FmsInitialCurrencyBalanceMapper initialCurrencyBalanceMapper;
    @Resource
    private FmsInitialBalanceMapper initialBalanceMapper;
    @Resource
    private FmsLedgerQueryMapper ledgerQueryMapper;

    @Resource
    private FmsAccountSetService accountSetService;
    @Resource
    @Lazy
    private FmsSubjectService subjectService;
    @Resource
    @Lazy
    private FmsClosingPeriodService closingPeriodService;
    @Resource
    @Lazy
    private FmsVoucherService voucherService;
    @Resource
    @Lazy
    private FmsVoucherWordService voucherWordService;
    @Resource
    @Lazy
    private FmsInitialBalanceService initialBalanceService;

    // ==================== 汇率表 ====================

    @Override
    public List<FmsCurrencyRateDO> getCurrencyRateList(Long accountSetId, Long currencyId, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        return currencyRateMapper.selectListByAccountSetId(accountSetId, currencyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCurrencyRate(FmsCurrencyRateSaveReqVO reqVO, Long userId) {
        accountSetService.validateAccountSetWritePermission(reqVO.getAccountSetId(), userId);
        validateForeignCurrency(reqVO.getAccountSetId(), reqVO.getCurrencyId());
        return upsertRate(reqVO.getAccountSetId(), reqVO.getCurrencyId(), reqVO.getMonth(),
                reqVO.getRate(), reqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCurrencyRate(Long accountSetId, Long id, Long userId) {
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        if (currencyRateMapper.selectByIdAndAccountSetId(id, accountSetId) == null) {
            throw exception(CURRENCY_RATE_NOT_EXISTS);
        }
        currencyRateMapper.deleteById(id);
    }

    @Override
    public List<FmsCurrencyRateRespVO> getMonthRateList(Long accountSetId, String month, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        List<FmsCurrencyRateDO> rates = currencyRateMapper.selectListByAccountSetId(accountSetId, null);
        List<FmsCurrencyRateRespVO> result = new ArrayList<>();
        for (FmsCurrencyDO currency : currencyMapper.selectListByAccountSetId(accountSetId)) {
            if (Boolean.TRUE.equals(currency.getStandard())) {
                continue;
            }
            result.add(resolveRate(currency, month, rates));
        }
        return result;
    }

    private FmsCurrencyRateRespVO resolveRate(FmsCurrencyDO currency, String month, List<FmsCurrencyRateDO> rates) {
        FmsCurrencyRateRespVO respVO = new FmsCurrencyRateRespVO();
        respVO.setCurrencyId(currency.getId());
        respVO.setCurrencyCode(currency.getCode());
        respVO.setCurrencyName(currency.getName());
        respVO.setMonth(month);
        // rates 已按期间倒序：取不晚于该期间的最近一条
        for (FmsCurrencyRateDO rate : rates) {
            if (ObjUtil.equal(rate.getCurrencyId(), currency.getId()) && rate.getMonth().compareTo(month) <= 0) {
                respVO.setId(rate.getMonth().equals(month) ? rate.getId() : null);
                respVO.setRate(rate.getRate());
                respVO.setSource(rate.getMonth().equals(month) ? 1 : 2);
                return respVO;
            }
        }
        respVO.setRate(currency.getExchangeRate());
        respVO.setSource(3);
        return respVO;
    }

    private Long upsertRate(Long accountSetId, Long currencyId, String month, BigDecimal rate, Long id) {
        BigDecimal value = rate.setScale(6, RoundingMode.HALF_UP);
        if (id != null) {
            FmsCurrencyRateDO old = currencyRateMapper.selectByIdAndAccountSetId(id, accountSetId);
            if (old == null) {
                throw exception(CURRENCY_RATE_NOT_EXISTS);
            }
            if (!old.getMonth().equals(month) || ObjUtil.notEqual(old.getCurrencyId(), currencyId)) {
                currencyRateMapper.deleteById(id);
            }
        }
        FmsCurrencyRateDO exist = currencyRateMapper.selectByMonth(accountSetId, currencyId, month);
        if (exist != null) {
            FmsCurrencyRateDO updateObj = new FmsCurrencyRateDO();
            updateObj.setId(exist.getId());
            updateObj.setRate(value);
            currencyRateMapper.updateById(updateObj);
            return exist.getId();
        }
        FmsCurrencyRateDO insertObj = new FmsCurrencyRateDO();
        insertObj.setAccountSetId(accountSetId);
        insertObj.setCurrencyId(currencyId);
        insertObj.setMonth(month);
        insertObj.setRate(value);
        currencyRateMapper.insert(insertObj);
        return insertObj.getId();
    }

    private FmsCurrencyDO validateForeignCurrency(Long accountSetId, Long currencyId) {
        FmsCurrencyDO currency = currencyMapper.selectByIdAndAccountSetId(currencyId, accountSetId);
        if (currency == null) {
            throw exception(CURRENCY_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(currency.getStandard())) {
            throw exception(CURRENCY_STANDARD_RATE_NOT_EDITABLE);
        }
        return currency;
    }

    // ==================== 外币期初 ====================

    @Override
    public List<FmsInitialCurrencyBalanceRespVO> getInitialCurrencyBalanceList(Long accountSetId, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        List<FmsSubjectDO> subjects = getForeignLeafSubjects(accountSetId, userId, null);
        Map<Long, FmsCurrencyDO> currencyMap = getCurrencyMap(accountSetId);
        Map<String, FmsInitialCurrencyBalanceDO> savedMap = getInitialCurrencyMap(accountSetId);
        Map<Long, FmsInitialBalanceDO> initialMap = convertMap(
                initialBalanceMapper.selectListByAccountSetId(accountSetId), FmsInitialBalanceDO::getSubjectId);
        List<FmsInitialCurrencyBalanceRespVO> result = new ArrayList<>();
        for (FmsSubjectDO subject : subjects) {
            for (Long currencyId : subject.getCurrencyIds()) {
                FmsCurrencyDO currency = currencyMap.get(currencyId);
                if (currency == null) {
                    continue;
                }
                FmsInitialCurrencyBalanceDO saved = savedMap.get(key(subject.getId(), currencyId));
                FmsInitialBalanceDO initial = initialMap.get(subject.getId());
                FmsInitialCurrencyBalanceRespVO row = new FmsInitialCurrencyBalanceRespVO();
                row.setId(saved == null ? null : saved.getId());
                row.setSubjectId(subject.getId());
                row.setSubjectCode(subject.getCode());
                row.setSubjectName(subject.getName());
                row.setBalanceDirection(subject.getBalanceDirection());
                row.setAuxiliaryAccounting(CollUtil.isNotEmpty(subject.getAuxiliaryTypeIds()));
                row.setCurrencyId(currencyId);
                row.setCurrencyCode(currency.getCode());
                row.setCurrencyName(currency.getName());
                row.setExchangeRate(currency.getExchangeRate());
                row.setOpeningOriginalAmount(saved == null ? ZERO : z(saved.getOpeningOriginalAmount()));
                row.setOpeningAmount(saved == null ? ZERO : z(saved.getOpeningAmount()));
                row.setYearDebitOriginalAmount(saved == null ? ZERO : z(saved.getYearDebitOriginalAmount()));
                row.setYearDebitAmount(saved == null ? ZERO : z(saved.getYearDebitAmount()));
                row.setYearCreditOriginalAmount(saved == null ? ZERO : z(saved.getYearCreditOriginalAmount()));
                row.setYearCreditAmount(saved == null ? ZERO : z(saved.getYearCreditAmount()));
                row.setSubjectOpeningAmount(initial == null ? ZERO : z(initial.getOpeningAmount()));
                result.add(row);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInitialCurrencyBalance(FmsInitialCurrencyBalanceSaveReqVO reqVO, Long userId) {
        // 1.1 校验权限和账套尚未结账（与【期初余额】一致：只能在启用期间修改）
        Long accountSetId = reqVO.getAccountSetId();
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        if (ObjUtil.notEqual(closingPeriodService.getCurrentMonth(accountSetId, accountSet.getStartTime()),
                YearMonth.from(accountSet.getStartTime()))) {
            throw exception(INITIAL_BALANCE_PERIOD_CLOSED);
        }
        // 1.2 校验科目和币别
        Map<Long, FmsSubjectDO> subjectMap = convertMap(getForeignLeafSubjects(accountSetId, userId, null),
                FmsSubjectDO::getId);
        for (FmsInitialCurrencyBalanceSaveReqVO.Balance balance : reqVO.getBalances()) {
            FmsSubjectDO subject = subjectMap.get(balance.getSubjectId());
            if (subject == null || !subject.getCurrencyIds().contains(balance.getCurrencyId())) {
                throw exception(INITIAL_CURRENCY_BALANCE_SUBJECT_INVALID,
                        subject == null ? String.valueOf(balance.getSubjectId()) : subject.getName());
            }
        }

        // 2. 逐行新增 / 更新 / 删除（全部为 0 的行删除）
        Map<String, FmsInitialCurrencyBalanceDO> savedMap = getInitialCurrencyMap(accountSetId);
        Set<Long> touchedSubjectIds = new LinkedHashSet<>();
        for (FmsInitialCurrencyBalanceSaveReqVO.Balance balance : reqVO.getBalances()) {
            touchedSubjectIds.add(balance.getSubjectId());
            FmsInitialCurrencyBalanceDO saved = savedMap.get(key(balance.getSubjectId(), balance.getCurrencyId()));
            FmsInitialCurrencyBalanceDO row = new FmsInitialCurrencyBalanceDO();
            row.setAccountSetId(accountSetId);
            row.setSubjectId(balance.getSubjectId());
            row.setCurrencyId(balance.getCurrencyId());
            row.setOpeningOriginalAmount(money(balance.getOpeningOriginalAmount()));
            row.setOpeningAmount(money(balance.getOpeningAmount()));
            row.setYearDebitOriginalAmount(money(balance.getYearDebitOriginalAmount()));
            row.setYearDebitAmount(money(balance.getYearDebitAmount()));
            row.setYearCreditOriginalAmount(money(balance.getYearCreditOriginalAmount()));
            row.setYearCreditAmount(money(balance.getYearCreditAmount()));
            boolean allZero = row.getOpeningOriginalAmount().signum() == 0 && row.getOpeningAmount().signum() == 0
                    && row.getYearDebitOriginalAmount().signum() == 0 && row.getYearDebitAmount().signum() == 0
                    && row.getYearCreditOriginalAmount().signum() == 0 && row.getYearCreditAmount().signum() == 0;
            if (saved != null && allZero) {
                initialCurrencyBalanceMapper.deleteById(saved.getId());
            } else if (saved != null) {
                row.setId(saved.getId());
                initialCurrencyBalanceMapper.updateById(row);
            } else if (!allZero) {
                initialCurrencyBalanceMapper.insert(row);
            }
        }

        // 3. 同步科目期初（本位币）：未启用辅助核算的外币科目，科目期初 = 各币别本位币期初合计
        Map<Long, List<FmsInitialCurrencyBalanceDO>> subjectRows = new HashMap<>();
        for (FmsInitialCurrencyBalanceDO row : initialCurrencyBalanceMapper.selectListByAccountSetId(accountSetId)) {
            subjectRows.computeIfAbsent(row.getSubjectId(), k -> new ArrayList<>()).add(row);
        }
        Map<Long, FmsInitialBalanceDO> initialMap = convertMap(
                initialBalanceMapper.selectListByAccountSetId(accountSetId), FmsInitialBalanceDO::getSubjectId);
        List<FmsInitialBalanceSaveReqVO.Balance> syncBalances = new ArrayList<>();
        for (Long subjectId : touchedSubjectIds) {
            FmsSubjectDO subject = subjectMap.get(subjectId);
            if (CollUtil.isNotEmpty(subject.getAuxiliaryTypeIds())) {
                continue;
            }
            List<FmsInitialCurrencyBalanceDO> rows = subjectRows.getOrDefault(subjectId, Collections.emptyList());
            BigDecimal opening = ZERO, yearDebit = ZERO, yearCredit = ZERO;
            for (FmsInitialCurrencyBalanceDO row : rows) {
                opening = opening.add(z(row.getOpeningAmount()));
                yearDebit = yearDebit.add(z(row.getYearDebitAmount()));
                yearCredit = yearCredit.add(z(row.getYearCreditAmount()));
            }
            boolean debit = ObjUtil.equal(subject.getBalanceDirection(), FmsDebitCreditDirectionEnum.DEBIT.getType());
            BigDecimal yearOpening = debit ? opening.subtract(yearDebit).add(yearCredit)
                    : opening.subtract(yearCredit).add(yearDebit);
            if (yearOpening.signum() < 0) {
                continue;
            }
            FmsInitialBalanceDO existing = initialMap.get(subjectId);
            FmsInitialBalanceSaveReqVO.Balance balance = new FmsInitialBalanceSaveReqVO.Balance();
            balance.setSubjectId(subjectId);
            balance.setOpeningAmount(opening);
            balance.setYearDebitAmount(yearDebit);
            balance.setYearCreditAmount(yearCredit);
            balance.setYearOpeningAmount(yearOpening);
            if (existing != null) {
                balance.setOpeningQuantity(existing.getOpeningQuantity());
                balance.setYearDebitQuantity(existing.getYearDebitQuantity());
                balance.setYearCreditQuantity(existing.getYearCreditQuantity());
                balance.setYearOpeningQuantity(existing.getYearOpeningQuantity());
            }
            syncBalances.add(balance);
        }
        if (CollUtil.isNotEmpty(syncBalances)) {
            FmsInitialBalanceSaveReqVO saveReqVO = new FmsInitialBalanceSaveReqVO();
            saveReqVO.setAccountSetId(accountSetId);
            saveReqVO.setBalances(syncBalances);
            initialBalanceService.saveInitialBalance(saveReqVO, userId);
        }
    }

    // ==================== 外币余额表 / 明细账 ====================

    @Override
    public List<FmsLedgerCurrencyBalanceRespVO> getCurrencyBalanceList(FmsLedgerCurrencyReqVO reqVO, Long userId) {
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetReadPermission(reqVO.getAccountSetId(), userId);
        YearMonth start = YearMonth.parse(reqVO.getStartMonth());
        YearMonth end = YearMonth.parse(reqVO.getEndMonth());
        if (start.isAfter(end)) {
            throw exception(LEDGER_PERIOD_INVALID);
        }
        List<Balance> balances = calculateBalances(accountSet, userId, start, end,
                reqVO.getSubjectId(), reqVO.getCurrencyId());
        List<FmsLedgerCurrencyBalanceRespVO> result = new ArrayList<>();
        for (Balance b : balances) {
            if (b.isEmpty()) {
                continue;
            }
            FmsLedgerCurrencyBalanceRespVO row = new FmsLedgerCurrencyBalanceRespVO();
            row.setSubjectId(b.subject.getId());
            row.setSubjectCode(b.subject.getCode());
            row.setSubjectName(b.subject.getName());
            row.setCurrencyId(b.currency.getId());
            row.setCurrencyCode(b.currency.getCode());
            row.setCurrencyName(b.currency.getName());
            String openingDirection = direction(b.openO, b.openA);
            row.setOpeningDirection(openingDirection);
            row.setOpeningOriginalAmount(directional(b.openO, openingDirection));
            row.setOpeningAmount(directional(b.openA, openingDirection));
            row.setPeriodDebitOriginalAmount(b.pDO);
            row.setPeriodDebitAmount(b.pDA);
            row.setPeriodCreditOriginalAmount(b.pCO);
            row.setPeriodCreditAmount(b.pCA);
            row.setYearDebitOriginalAmount(b.yDO);
            row.setYearDebitAmount(b.yDA);
            row.setYearCreditOriginalAmount(b.yCO);
            row.setYearCreditAmount(b.yCA);
            BigDecimal endO = b.endingOriginal(), endA = b.endingAmount();
            String endingDirection = direction(endO, endA);
            row.setEndingDirection(endingDirection);
            row.setEndingOriginalAmount(directional(endO, endingDirection));
            row.setEndingAmount(directional(endA, endingDirection));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<FmsLedgerCurrencyDetailRespVO> getCurrencyDetailList(FmsLedgerCurrencyReqVO reqVO, Long userId) {
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetReadPermission(reqVO.getAccountSetId(), userId);
        YearMonth start = YearMonth.parse(reqVO.getStartMonth());
        YearMonth end = YearMonth.parse(reqVO.getEndMonth());
        if (start.isAfter(end)) {
            throw exception(LEDGER_PERIOD_INVALID);
        }
        List<Balance> balances = calculateBalances(accountSet, userId, start, end,
                reqVO.getSubjectId(), reqVO.getCurrencyId());
        if (balances.isEmpty()) {
            return Collections.emptyList();
        }
        Balance b = balances.get(0);
        List<FmsLedgerCurrencyDetailRespVO> rows = new ArrayList<>();
        BigDecimal balO = b.openO, balA = b.openA;
        rows.add(detailRow(1, start.toString(), start.atDay(1).format(DATE_FORMATTER), "期初余额",
                null, ZERO, ZERO, ZERO, ZERO, balO, balA));
        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            LocalDateTime monthStart = month.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();
            BigDecimal mDO = ZERO, mDA = ZERO, mCO = ZERO, mCA = ZERO;
            for (FmsLedgerEntryVO e : b.entries) {
                if (e.getVoucherTime().isBefore(monthStart) || !e.getVoucherTime().isBefore(monthEnd)) {
                    continue;
                }
                boolean debit = isDebit(e);
                BigDecimal o = z(e.getOriginalAmount());
                BigDecimal dO = debit ? o : ZERO, dA = z(e.getDebitAmount());
                BigDecimal cO = debit ? ZERO : o, cA = z(e.getCreditAmount());
                balO = balO.add(dO).subtract(cO);
                balA = balA.add(dA).subtract(cA);
                mDO = mDO.add(dO); mDA = mDA.add(dA); mCO = mCO.add(cO); mCA = mCA.add(cA);
                FmsLedgerCurrencyDetailRespVO row = detailRow(2, month.toString(),
                        e.getVoucherTime().format(DATE_FORMATTER), e.getDigest(), e.getExchangeRate(),
                        dO, dA, cO, cA, balO, balA);
                row.setVoucherId(e.getVoucherId());
                row.setVoucherNumber(StrUtil.blankToDefault(e.getVoucherWordName(), "") + "-" + e.getVoucherNumber());
                rows.add(row);
            }
            rows.add(detailRow(3, month.toString(), month.atEndOfMonth().format(DATE_FORMATTER), "本月合计",
                    null, mDO, mDA, mCO, mCA, balO, balA));
            // 本年累计：当年 1 月至本月的发生额（启用年度加上外币期初里录入的本年累计）
            BigDecimal yDO = ZERO, yDA = ZERO, yCO = ZERO, yCA = ZERO;
            if (b.initial != null && accountSet.getStartTime().getYear() == month.getYear()) {
                yDO = z(b.initial.getYearDebitOriginalAmount());
                yDA = z(b.initial.getYearDebitAmount());
                yCO = z(b.initial.getYearCreditOriginalAmount());
                yCA = z(b.initial.getYearCreditAmount());
            }
            LocalDateTime yearStart = LocalDate.of(month.getYear(), 1, 1).atStartOfDay();
            for (FmsLedgerEntryVO e : b.entries) {
                if (e.getVoucherTime().isBefore(yearStart) || !e.getVoucherTime().isBefore(monthEnd)) {
                    continue;
                }
                if (isDebit(e)) {
                    yDO = yDO.add(z(e.getOriginalAmount()));
                    yDA = yDA.add(z(e.getDebitAmount()));
                } else {
                    yCO = yCO.add(z(e.getOriginalAmount()));
                    yCA = yCA.add(z(e.getCreditAmount()));
                }
            }
            rows.add(detailRow(4, month.toString(), month.atEndOfMonth().format(DATE_FORMATTER), "本年累计",
                    null, yDO, yDA, yCO, yCA, balO, balA));
        }
        return rows;
    }

    private FmsLedgerCurrencyDetailRespVO detailRow(int rowType, String period, String date, String digest,
            BigDecimal rate, BigDecimal dO, BigDecimal dA, BigDecimal cO, BigDecimal cA,
            BigDecimal balO, BigDecimal balA) {
        FmsLedgerCurrencyDetailRespVO row = new FmsLedgerCurrencyDetailRespVO();
        row.setRowType(rowType);
        row.setPeriod(period);
        row.setAccountDate(date);
        row.setDigest(digest);
        row.setExchangeRate(rate);
        row.setDebitOriginalAmount(dO);
        row.setDebitAmount(dA);
        row.setCreditOriginalAmount(cO);
        row.setCreditAmount(cA);
        String dir = direction(balO, balA);
        row.setBalanceDirection(dir);
        row.setBalanceOriginalAmount(directional(balO, dir));
        row.setBalanceAmount(directional(balA, dir));
        return row;
    }

    // ==================== 期末调汇 ====================

    @Override
    public FmsExchangeAdjustRespVO previewExchangeAdjust(FmsExchangeAdjustReqVO reqVO, Long userId) {
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetReadPermission(reqVO.getAccountSetId(), userId);
        Long accountSetId = reqVO.getAccountSetId();
        YearMonth month = closingPeriodService.getCurrentMonth(accountSetId, accountSet.getStartTime());

        // 1. 汇兑损益科目
        List<FmsSubjectDO> allSubjects = subjectService.getSubjectList(accountSetId, null, userId);
        Set<Long> parentIds = convertSet(allSubjects, FmsSubjectDO::getParentId);
        FmsSubjectDO profitLossSubject = null;
        if (reqVO.getProfitLossSubjectId() != null) {
            profitLossSubject = CollUtil.findOne(allSubjects, s -> s.getId().equals(reqVO.getProfitLossSubjectId()));
        } else {
            profitLossSubject = allSubjects.stream()
                    .filter(s -> "汇兑损益".equals(s.getName()) && !parentIds.contains(s.getId()))
                    .min(Comparator.comparing(FmsSubjectDO::getCode)).orElse(null);
        }

        // 2. 期末汇率：页面填写的优先，其次汇率表 / 默认汇率
        Map<Long, BigDecimal> inputRates = new HashMap<>();
        if (reqVO.getRates() != null) {
            reqVO.getRates().forEach(r -> {
                if (r.getRate() != null) {
                    inputRates.put(r.getCurrencyId(), r.getRate());
                }
            });
        }
        List<FmsCurrencyRateRespVO> rates = getMonthRateList(accountSetId, month.toString(), userId);
        for (FmsCurrencyRateRespVO rate : rates) {
            if (inputRates.containsKey(rate.getCurrencyId())) {
                rate.setRate(inputRates.get(rate.getCurrencyId()));
            }
        }
        Map<Long, FmsCurrencyRateRespVO> rateMap = convertMap(rates, FmsCurrencyRateRespVO::getCurrencyId);

        // 3. 逐个外币科目 × 币别计算汇兑差额
        List<FmsExchangeAdjustRespVO.Line> lines = new ArrayList<>();
        BigDecimal total = ZERO;
        for (Balance b : calculateBalances(accountSet, userId, month, month, null, null)) {
            BigDecimal endO = b.endingOriginal(), endA = b.endingAmount();
            if (endO.signum() == 0 && endA.signum() == 0) {
                continue;
            }
            FmsCurrencyRateRespVO rate = rateMap.get(b.currency.getId());
            BigDecimal rateValue = rate == null ? null : rate.getRate();
            if (rateValue == null || rateValue.signum() <= 0) {
                throw exception(EXCHANGE_ADJUST_RATE_INVALID, b.currency.getName());
            }
            BigDecimal adjusted = endO.multiply(rateValue).setScale(2, RoundingMode.HALF_UP);
            BigDecimal diff = adjusted.subtract(endA);
            String dir = direction(endO, endA);
            FmsExchangeAdjustRespVO.Line line = new FmsExchangeAdjustRespVO.Line();
            line.setSubjectId(b.subject.getId());
            line.setSubjectCode(b.subject.getCode());
            line.setSubjectName(b.subject.getName());
            line.setCurrencyId(b.currency.getId());
            line.setCurrencyCode(b.currency.getCode());
            line.setCurrencyName(b.currency.getName());
            line.setRate(rateValue);
            line.setDirection(dir);
            line.setEndingOriginalAmount(directional(endO, dir));
            line.setEndingAmount(directional(endA, dir));
            line.setAdjustedAmount(directional(adjusted, dir));
            line.setDifferenceAmount(diff);
            lines.add(line);
            total = total.add(diff);
        }

        FmsExchangeAdjustRespVO respVO = new FmsExchangeAdjustRespVO();
        respVO.setMonth(month.toString());
        if (profitLossSubject != null) {
            respVO.setProfitLossSubjectId(profitLossSubject.getId());
            respVO.setProfitLossSubjectCode(profitLossSubject.getCode());
            respVO.setProfitLossSubjectName(profitLossSubject.getName());
        }
        respVO.setRates(rates);
        respVO.setLines(lines);
        respVO.setTotalDifference(total);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FmsExchangeAdjustRespVO generateExchangeAdjust(FmsExchangeAdjustReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        FmsExchangeAdjustRespVO preview = previewExchangeAdjust(reqVO, userId);
        List<FmsExchangeAdjustRespVO.Line> lines = filterList(preview.getLines(),
                line -> line.getDifferenceAmount().signum() != 0);
        if (lines.isEmpty()) {
            throw exception(EXCHANGE_ADJUST_NOTHING);
        }
        // 1. 校验汇兑损益科目
        List<FmsSubjectDO> allSubjects = subjectService.getSubjectList(accountSetId, null, userId);
        Set<Long> parentIds = convertSet(allSubjects, FmsSubjectDO::getParentId);
        FmsSubjectDO profitLossSubject = preview.getProfitLossSubjectId() == null ? null
                : CollUtil.findOne(allSubjects, s -> s.getId().equals(preview.getProfitLossSubjectId()));
        if (profitLossSubject == null || parentIds.contains(profitLossSubject.getId())
                || ObjUtil.notEqual(profitLossSubject.getStatus(), CommonStatusEnum.ENABLE.getStatus())
                || CollUtil.isNotEmpty(profitLossSubject.getCurrencyIds())) {
            throw exception(EXCHANGE_ADJUST_SUBJECT_INVALID);
        }

        // 2. 把本次使用的期末汇率写入汇率表
        Set<Long> usedCurrencyIds = convertSet(lines, FmsExchangeAdjustRespVO.Line::getCurrencyId);
        for (FmsCurrencyRateRespVO rate : preview.getRates()) {
            if (usedCurrencyIds.contains(rate.getCurrencyId())) {
                upsertRate(accountSetId, rate.getCurrencyId(), preview.getMonth(), rate.getRate(), null);
            }
        }

        // 3. 生成调汇凭证：外币科目分录原币为 0，只调整本位币；差额合计记入汇兑损益
        YearMonth month = YearMonth.parse(preview.getMonth());
        LocalDateTime voucherTime = month.atEndOfMonth().atStartOfDay();
        Long voucherWordId = reqVO.getVoucherWordId();
        if (voucherWordId == null) {
            List<FmsVoucherWordDO> words = voucherWordService.getVoucherWordList(accountSetId);
            FmsVoucherWordDO word = CollUtil.findOne(words, w -> Boolean.TRUE.equals(w.getDefaultStatus()));
            voucherWordId = word != null ? word.getId() : CollUtil.getFirst(words).getId();
        }
        List<FmsVoucherEntrySaveReqVO> entries = new ArrayList<>();
        BigDecimal total = ZERO;
        for (FmsExchangeAdjustRespVO.Line line : lines) {
            FmsVoucherEntrySaveReqVO entry = new FmsVoucherEntrySaveReqVO();
            entry.setDigest("期末调汇：" + line.getCurrencyName() + " 期末汇率 "
                    + line.getRate().stripTrailingZeros().toPlainString());
            entry.setSubjectId(line.getSubjectId());
            entry.setCurrencyId(line.getCurrencyId());
            entry.setExchangeRate(line.getRate());
            entry.setOriginalAmount(ZERO);
            BigDecimal diff = line.getDifferenceAmount();
            entry.setDebitAmount(diff.signum() > 0 ? diff : ZERO);
            entry.setCreditAmount(diff.signum() < 0 ? diff.negate() : ZERO);
            entry.setAuxiliaries(Collections.emptyList());
            entries.add(entry);
            total = total.add(diff);
        }
        if (total.signum() != 0) {
            FmsVoucherEntrySaveReqVO entry = new FmsVoucherEntrySaveReqVO();
            entry.setDigest(total.signum() > 0 ? "期末调汇：汇兑收益" : "期末调汇：汇兑损失");
            entry.setSubjectId(profitLossSubject.getId());
            entry.setDebitAmount(total.signum() < 0 ? total.negate() : ZERO);
            entry.setCreditAmount(total.signum() > 0 ? total : ZERO);
            entry.setAuxiliaries(Collections.emptyList());
            entries.add(entry);
        }
        FmsVoucherSaveReqVO createReqVO = new FmsVoucherSaveReqVO();
        createReqVO.setAccountSetId(accountSetId);
        createReqVO.setVoucherWordId(voucherWordId);
        createReqVO.setVoucherTime(voucherTime);
        createReqVO.setVoucherNumber(voucherService.getNextVoucherNumber(accountSetId, voucherWordId, voucherTime, userId));
        createReqVO.setAttachmentCount(0);
        createReqVO.setEntries(entries);
        Long voucherId = voucherService.createVoucher(createReqVO, userId);
        preview.setVoucherId(voucherId);
        return preview;
    }

    // ==================== 公共计算 ====================

    /**
     * 外币科目 × 币别的余额（内部统一用“借方为正”的带符号金额）
     */
    private static class Balance {
        FmsSubjectDO subject;
        FmsCurrencyDO currency;
        FmsInitialCurrencyBalanceDO initial;
        List<FmsLedgerEntryVO> entries = new ArrayList<>();
        BigDecimal openO = ZERO, openA = ZERO;
        BigDecimal pDO = ZERO, pDA = ZERO, pCO = ZERO, pCA = ZERO;
        BigDecimal yDO = ZERO, yDA = ZERO, yCO = ZERO, yCA = ZERO;

        BigDecimal endingOriginal() {
            return openO.add(pDO).subtract(pCO);
        }

        BigDecimal endingAmount() {
            return openA.add(pDA).subtract(pCA);
        }

        boolean isEmpty() {
            return openO.signum() == 0 && openA.signum() == 0 && pDO.signum() == 0 && pDA.signum() == 0
                    && pCO.signum() == 0 && pCA.signum() == 0 && yDO.signum() == 0 && yDA.signum() == 0
                    && yCO.signum() == 0 && yCA.signum() == 0;
        }
    }

    private List<Balance> calculateBalances(FmsAccountSetDO accountSet, Long userId, YearMonth start, YearMonth end,
            Long subjectId, Long currencyId) {
        Long accountSetId = accountSet.getId();
        List<FmsSubjectDO> subjects = getForeignLeafSubjects(accountSetId, userId, subjectId);
        Map<Long, FmsCurrencyDO> currencyMap = getCurrencyMap(accountSetId);
        Map<String, FmsInitialCurrencyBalanceDO> initialMap = getInitialCurrencyMap(accountSetId);
        LocalDateTime startTime = start.atDay(1).atStartOfDay();
        LocalDateTime endTime = end.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime yearStart = LocalDate.of(end.getYear(), 1, 1).atStartOfDay();
        Map<String, List<FmsLedgerEntryVO>> entryMap = new HashMap<>();
        for (FmsLedgerEntryVO entry : ledgerQueryMapper.selectEntryListBeforeTime(accountSetId, endTime)) {
            if (entry.getCurrencyId() != null) {
                entryMap.computeIfAbsent(key(entry.getSubjectId(), entry.getCurrencyId()), k -> new ArrayList<>())
                        .add(entry);
            }
        }
        List<Balance> result = new ArrayList<>();
        for (FmsSubjectDO subject : subjects) {
            int sign = ObjUtil.equal(subject.getBalanceDirection(), FmsDebitCreditDirectionEnum.DEBIT.getType()) ? 1 : -1;
            for (Long subjectCurrencyId : subject.getCurrencyIds()) {
                FmsCurrencyDO currency = currencyMap.get(subjectCurrencyId);
                if (currency == null || (currencyId != null && !currencyId.equals(subjectCurrencyId))) {
                    continue;
                }
                Balance b = new Balance();
                b.subject = subject;
                b.currency = currency;
                b.initial = initialMap.get(key(subject.getId(), subjectCurrencyId));
                if (b.initial != null) {
                    b.openO = z(b.initial.getOpeningOriginalAmount()).multiply(BigDecimal.valueOf(sign));
                    b.openA = z(b.initial.getOpeningAmount()).multiply(BigDecimal.valueOf(sign));
                    if (accountSet.getStartTime().getYear() == end.getYear()) {
                        b.yDO = z(b.initial.getYearDebitOriginalAmount());
                        b.yDA = z(b.initial.getYearDebitAmount());
                        b.yCO = z(b.initial.getYearCreditOriginalAmount());
                        b.yCA = z(b.initial.getYearCreditAmount());
                    }
                }
                b.entries = entryMap.getOrDefault(key(subject.getId(), subjectCurrencyId), Collections.emptyList());
                for (FmsLedgerEntryVO e : b.entries) {
                    boolean debit = isDebit(e);
                    BigDecimal o = z(e.getOriginalAmount());
                    if (e.getVoucherTime().isBefore(startTime)) {
                        b.openO = b.openO.add(debit ? o : o.negate());
                        b.openA = b.openA.add(z(e.getDebitAmount())).subtract(z(e.getCreditAmount()));
                    } else if (debit) {
                        b.pDO = b.pDO.add(o);
                        b.pDA = b.pDA.add(z(e.getDebitAmount()));
                    } else {
                        b.pCO = b.pCO.add(o);
                        b.pCA = b.pCA.add(z(e.getCreditAmount()));
                    }
                    if (!e.getVoucherTime().isBefore(yearStart)) {
                        if (debit) {
                            b.yDO = b.yDO.add(o);
                            b.yDA = b.yDA.add(z(e.getDebitAmount()));
                        } else {
                            b.yCO = b.yCO.add(o);
                            b.yCA = b.yCA.add(z(e.getCreditAmount()));
                        }
                    }
                }
                result.add(b);
            }
        }
        return result;
    }

    private List<FmsSubjectDO> getForeignLeafSubjects(Long accountSetId, Long userId, Long subjectId) {
        List<FmsSubjectDO> subjects = subjectService.getSubjectList(accountSetId, null, userId);
        Set<Long> parentIds = convertSet(subjects, FmsSubjectDO::getParentId);
        List<FmsSubjectDO> result = filterList(subjects, s -> CollUtil.isNotEmpty(s.getCurrencyIds())
                && !parentIds.contains(s.getId()) && (subjectId == null || subjectId.equals(s.getId())));
        result.sort(Comparator.comparing(FmsSubjectDO::getCode));
        return result;
    }

    private Map<Long, FmsCurrencyDO> getCurrencyMap(Long accountSetId) {
        return convertMap(filterList(currencyMapper.selectListByAccountSetId(accountSetId),
                c -> !Boolean.TRUE.equals(c.getStandard())), FmsCurrencyDO::getId);
    }

    private Map<String, FmsInitialCurrencyBalanceDO> getInitialCurrencyMap(Long accountSetId) {
        return convertMap(initialCurrencyBalanceMapper.selectListByAccountSetId(accountSetId),
                row -> key(row.getSubjectId(), row.getCurrencyId()));
    }

    private static boolean isDebit(FmsLedgerEntryVO entry) {
        return z(entry.getDebitAmount()).signum() != 0;
    }

    private static String key(Long subjectId, Long currencyId) {
        return subjectId + "_" + currencyId;
    }

    private static BigDecimal z(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return z(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 余额方向：以本位币为准，本位币为 0 时看原币
     */
    private static String direction(BigDecimal original, BigDecimal amount) {
        int signum = amount.signum() != 0 ? amount.signum() : original.signum();
        return signum > 0 ? "借" : signum < 0 ? "贷" : "平";
    }

    private static BigDecimal directional(BigDecimal value, String direction) {
        return "贷".equals(direction) ? value.negate() : value;
    }

}
