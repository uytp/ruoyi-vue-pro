package cn.iocoder.yudao.module.fms.service.cash;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils;
import cn.iocoder.yudao.module.fms.controller.admin.cash.vo.*;
import cn.iocoder.yudao.module.fms.dal.dataobject.cash.FmsBankReconcileDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.cash.FmsBankStatementDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAccountSetDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherEntryDO;
import cn.iocoder.yudao.module.fms.dal.mysql.cash.FmsBankReconcileMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.cash.FmsBankStatementMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.voucher.FmsVoucherEntryMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.voucher.FmsVoucherMapper;
import cn.iocoder.yudao.module.fms.service.closing.FmsClosingPeriodService;
import cn.iocoder.yudao.module.fms.service.config.FmsAccountSetService;
import cn.iocoder.yudao.module.fms.service.config.FmsSubjectService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.filterList;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;

/**
 * FMS 出纳日记账与银行对账 Service 实现
 */
@Service
@Validated
public class FmsCashBankServiceImpl implements FmsCashBankService {

    /** 库存现金科目编码 */
    private static final String CASH_CODE = "1001";
    /** 银行存款科目编码前缀 */
    private static final String BANK_CODE = "1002";
    /** 自动勾对、未达项配对的日期容差（天） */
    private static final int MATCH_DAYS = 3;

    @Resource
    private FmsVoucherEntryMapper voucherEntryMapper;
    @Resource
    private FmsVoucherMapper voucherMapper;
    @Resource
    private FmsBankStatementMapper bankStatementMapper;
    @Resource
    private FmsBankReconcileMapper bankReconcileMapper;
    @Resource
    private FmsAccountSetService accountSetService;
    @Resource
    private FmsSubjectService subjectService;
    @Resource
    @Lazy
    private FmsClosingPeriodService closingPeriodService;

    // ==================== 公共方法 ====================

    /**
     * 校验科目是库存现金或银行存款的末级科目
     */
    private FmsSubjectDO validateCashBankSubject(Long accountSetId, Long subjectId, Long userId) {
        FmsSubjectDO subject = subjectService.getSubject(accountSetId, subjectId, userId);
        if (subject == null || (!subject.getCode().equals(CASH_CODE) && !subject.getCode().startsWith(BANK_CODE))) {
            throw exception(CASH_ENTRY_SUBJECT_INVALID);
        }
        return subject;
    }

    /**
     * 校验科目是银行存款的末级科目（导入对账单和生成调节表只适用于银行）
     */
    private FmsSubjectDO validateBankSubject(Long accountSetId, Long subjectId, Long userId) {
        FmsSubjectDO subject = subjectService.getSubject(accountSetId, subjectId, userId);
        if (subject == null || !subject.getCode().startsWith(BANK_CODE)) {
            throw exception(CASH_BANK_SUBJECT_INVALID);
        }
        return subject;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 期初余额：按科目余额方向返回指定日期之前的分录余额累计
     */
    private BigDecimal getOpeningBalance(Long accountSetId, FmsSubjectDO subject, LocalDateTime before, Long userId) {
        BigDecimal balance = BigDecimal.ZERO;
        for (FmsVoucherEntryDO entry : voucherEntryMapper.selectListBySubjectBefore(accountSetId, subject.getId(), before)) {
            balance = balance.add(nullToZero(entry.getDebitAmount())).subtract(nullToZero(entry.getCreditAmount()));
        }
        return balance;
    }

    @Override
    public List<FmsCashJournalRespVO> getCashJournalList(FmsCashJournalReqVO reqVO, Long userId) {
        // 1. 校验账套读权限与科目是否为出纳科目
        accountSetService.validateAccountSetReadPermission(reqVO.getAccountSetId(), userId);
        FmsSubjectDO subject = validateCashBankSubject(reqVO.getAccountSetId(), reqVO.getSubjectId(), userId);
        YearMonth month = LocalDateTimeUtils.parseYearMonth(reqVO.getMonth());

        // 2. 取该科目在期间内的分录（不含作废凭证），按凭证顺序排列
        LocalDateTime begin = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        List<FmsVoucherEntryDO> entries = voucherEntryMapper.selectListBySubjectAndPeriod(
                reqVO.getAccountSetId(), subject.getId(), begin, end);

        // 3. 拼接余额（以本期期初余额为起点）
        BigDecimal balance = getOpeningBalance(reqVO.getAccountSetId(), subject, begin, userId);

        // 凭证号在凭证主表上，分录对象里没有，这里按凭证 id 批量查出再映射
        Map<Long, Integer> voucherNumberMap = convertMap(
                voucherMapper.selectListByIdsAndAccountSetId(
                        convertSet(entries, FmsVoucherEntryDO::getVoucherId), reqVO.getAccountSetId()),
                FmsVoucherDO::getId, FmsVoucherDO::getVoucherNumber);

        List<FmsCashJournalRespVO> result = new ArrayList<>();
        for (FmsVoucherEntryDO entry : entries) {
            balance = balance.add(nullToZero(entry.getDebitAmount()))
                    .subtract(nullToZero(entry.getCreditAmount()));
            FmsCashJournalRespVO respVO = new FmsCashJournalRespVO();
            respVO.setEntryId(entry.getId());
            respVO.setVoucherId(entry.getVoucherId());
            respVO.setVoucherTime(entry.getVoucherTime());
            Integer number = voucherNumberMap.get(entry.getVoucherId());
            respVO.setVoucherNo(number == null ? null : "记-" + number);
            respVO.setDigest(entry.getDigest());
            respVO.setDebitAmount(nullToZero(entry.getDebitAmount()));
            respVO.setCreditAmount(nullToZero(entry.getCreditAmount()));
            respVO.setBalance(balance);
            respVO.setCashReconciled(Boolean.TRUE.equals(entry.getCashReconciled()));
            result.add(respVO);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCashReconciled(FmsCashReconcileReqVO reqVO, Long userId) {
        // 1. 校验账套写权限
        accountSetService.validateAccountSetWritePermission(reqVO.getAccountSetId(), userId);
        // 2. 校验分录均属于该账套、且科目为库存现金或银行存款
        List<FmsVoucherEntryDO> entries = voucherEntryMapper.selectListByIdsAndAccountSetId(
                reqVO.getEntryIds(), reqVO.getAccountSetId());
        if (entries.size() != new HashSet<>(reqVO.getEntryIds()).size()) {
            throw exception(CASH_ENTRY_NOT_EXISTS);
        }
        Set<Long> subjectIds = convertSet(entries, FmsVoucherEntryDO::getSubjectId);
        for (Long subjectId : subjectIds) {
            validateCashBankSubject(reqVO.getAccountSetId(), subjectId, userId);
        }
        // 3. 逐条更新勾对标记（已结账期间的校验由结账模块的规则保证，此处不改变其逻辑）
        for (FmsVoucherEntryDO entry : entries) {
            voucherEntryMapper.updateById(new FmsVoucherEntryDO()
                    .setId(entry.getId()).setCashReconciled(reqVO.getReconciled()));
        }
    }

    @Override
    public int importBankStatement(Long accountSetId, Long subjectId, List<FmsBankStatementImportVO> rows, Long userId) {
        // 1. 校验账套写权限与银行科目
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        validateBankSubject(accountSetId, subjectId, userId);
        if (CollUtil.isEmpty(rows)) {
            throw exception(BANK_STATEMENT_IMPORT_EMPTY);
        }
        // 2. 逐行校验：日期、流水号必填；收入与支出必须二选一
        int rowNo = 1;
        for (FmsBankStatementImportVO row : rows) {
            rowNo++;
            if (row.getStatementDate() == null || row.getReferenceNo() == null || row.getReferenceNo().isBlank()) {
                throw exception(BANK_STATEMENT_IMPORT_ROW_INVALID, rowNo, "日期或流水号为空");
            }
            boolean hasDebit = nullToZero(row.getDebitAmount()).signum() > 0;
            boolean hasCredit = nullToZero(row.getCreditAmount()).signum() > 0;
            if (hasDebit == hasCredit) {
                throw exception(BANK_STATEMENT_IMPORT_ROW_INVALID, rowNo, "收入和支出必须二选一");
            }
        }
        // 3. 流水号在该银行科目下不可重复（文件内与已导入的都不允许）
        List<String> refNos = convertSet(rows, FmsBankStatementImportVO::getReferenceNo).stream().toList();
        List<FmsBankStatementDO> duplicated = bankStatementMapper.selectListByReferenceNos(accountSetId, refNos);
        if (CollUtil.isNotEmpty(duplicated)) {
            throw exception(BANK_STATEMENT_DUPLICATE, duplicated.get(0).getReferenceNo());
        }
        // 4. 同一批次写入同一个批次号，便于追溯
        String batch = UUID.randomUUID().toString().replace("-", "");
        for (FmsBankStatementImportVO row : rows) {
            FmsBankStatementDO statement = new FmsBankStatementDO()
                    .setAccountSetId(accountSetId).setSubjectId(subjectId)
                    .setStatementDate(row.getStatementDate()).setSummary(row.getSummary())
                    .setDebitAmount(nullToZero(row.getDebitAmount()))
                    .setCreditAmount(nullToZero(row.getCreditAmount()))
                    .setBalance(row.getBalance()).setReferenceNo(row.getReferenceNo())
                    .setImportBatch(batch).setReconciled(false);
            bankStatementMapper.insert(statement);
        }
        return rows.size();
    }

    @Override
    public List<FmsBankStatementRespVO> getBankStatementList(FmsBankStatementQueryReqVO reqVO, Long userId) {
        accountSetService.validateAccountSetReadPermission(reqVO.getAccountSetId(), userId);
        YearMonth month = LocalDateTimeUtils.parseYearMonth(reqVO.getMonth());
        List<FmsBankStatementDO> statements = bankStatementMapper.selectListByAccountSetIdAndSubjectId(
                reqVO.getAccountSetId(), reqVO.getSubjectId(), month.atDay(1), month.atEndOfMonth());
        List<FmsBankStatementRespVO> result = new ArrayList<>();
        for (FmsBankStatementDO statement : statements) {
            FmsBankStatementRespVO respVO = new FmsBankStatementRespVO();
            respVO.setId(statement.getId());
            respVO.setStatementDate(statement.getStatementDate());
            respVO.setSummary(statement.getSummary());
            respVO.setDebitAmount(statement.getDebitAmount());
            respVO.setCreditAmount(statement.getCreditAmount());
            respVO.setBalance(statement.getBalance());
            respVO.setReferenceNo(statement.getReferenceNo());
            respVO.setReconciled(Boolean.TRUE.equals(statement.getReconciled()));
            respVO.setVoucherEntryId(statement.getVoucherEntryId());
            result.add(respVO);
        }
        return result;
    }

    @Override
    public int autoReconcile(Long accountSetId, Long subjectId, String month, Long userId) {
        // 1. 校验账套写权限与银行科目；已结账期间不允许改动对账状态
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        validateBankSubject(accountSetId, subjectId, userId);
        YearMonth ym = LocalDateTimeUtils.parseYearMonth(month);
        if (closingPeriodService.isPeriodClosed(accountSetId, month)) {
            throw exception(BANK_RECONCILE_PERIOD_CLOSED);
        }
        // 2. 候选集：对账单前后各放宽 MATCH_DAYS 天；分录取本期内；仅未勾对的参与配对
        List<FmsBankStatementDO> statements = filterList(
                bankStatementMapper.selectListByAccountSetIdAndSubjectId(accountSetId, subjectId,
                        ym.atDay(1).minusDays(MATCH_DAYS), ym.atEndOfMonth().plusDays(MATCH_DAYS)),
                s -> !Boolean.TRUE.equals(s.getReconciled()));
        List<FmsVoucherEntryDO> entries = filterList(
                voucherEntryMapper.selectListBySubjectAndPeriod(accountSetId, subjectId,
                        ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay()),
                e -> !Boolean.TRUE.equals(e.getCashReconciled()) && e.getVoucherTime() != null);
        // 3. 一一配对：金额方向一致、金额相等、日期差在容差内，且每条只能用一次
        int matched = 0;
        Set<Long> usedStatements = new HashSet<>();
        Set<Long> usedEntries = new HashSet<>();
        for (FmsVoucherEntryDO entry : entries) {
            BigDecimal entryAmount = nullToZero(entry.getDebitAmount()).subtract(nullToZero(entry.getCreditAmount()));
            for (FmsBankStatementDO statement : statements) {
                if (usedStatements.contains(statement.getId())) {
                    continue;
                }
                BigDecimal stmtAmount = nullToZero(statement.getDebitAmount()).subtract(nullToZero(statement.getCreditAmount()));
                if (stmtAmount.compareTo(entryAmount) != 0) {
                    continue;
                }
                long days = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(
                        entry.getVoucherTime().toLocalDate(), statement.getStatementDate()));
                if (days > MATCH_DAYS) {
                    continue;
                }
                usedStatements.add(statement.getId());
                usedEntries.add(entry.getId());
                // 4. 双向更新勾对标记
                bankStatementMapper.updateById(new FmsBankStatementDO()
                        .setId(statement.getId()).setReconciled(true).setVoucherEntryId(entry.getId()));
                voucherEntryMapper.updateById(new FmsVoucherEntryDO()
                        .setId(entry.getId()).setCashReconciled(true));
                matched++;
                break;
            }
        }
        return matched;
    }

    @Override
    public FmsBankReconcileRespVO calculateBankReconcile(Long accountSetId, Long subjectId, String month, Long userId) {
        // 1. 权限与银行科目校验
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        validateBankSubject(accountSetId, subjectId, userId);
        YearMonth ym = LocalDateTimeUtils.parseYearMonth(month);
        FmsSubjectDO subject = subjectService.getSubject(accountSetId, subjectId, userId);

        // 2. 日记账余额：截至本期末的分录累计（含期初），未勾对分录仍计入日记账
        LocalDateTime begin = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        BigDecimal bookBalance = getOpeningBalance(accountSetId, subject, end, userId);
        List<FmsVoucherEntryDO> entries = voucherEntryMapper.selectListBySubjectAndPeriod(
                accountSetId, subjectId, begin, end);

        // 3. 银行余额：截至期末最后一行的余额（时点值，不受期间限制）
        List<FmsBankStatementDO> statements = bankStatementMapper.selectListByAccountSetIdAndSubjectId(
                accountSetId, subjectId, null, ym.atEndOfMonth());
        BigDecimal bankBalance = statements.isEmpty() ? BigDecimal.ZERO
                : nullToZero(statements.get(statements.size() - 1).getBalance());
        // 未达账项只取本期内的对账单（历史期间已结账，其未勾对行不应再进入本期）
        List<FmsBankStatementDO> periodStatements = bankStatementMapper.selectListByAccountSetIdAndSubjectId(
                accountSetId, subjectId, ym.atDay(1), ym.atEndOfMonth());

        // 4a. 已结账期间：双方都有、金额相同、日期差在容差内但未勾对的配对，不计入未达项，单独列示
        boolean periodClosed = closingPeriodService.isPeriodClosed(accountSetId, month);
        Set<Long> pairedEntryIds = new HashSet<>();
        Set<Long> pairedStatementIds = new HashSet<>();
        List<FmsUnreconciledSamePeriodItemRespVO> samePeriodItems = new ArrayList<>();
        if (periodClosed) {
            for (FmsVoucherEntryDO entry : entries) {
                if (Boolean.TRUE.equals(entry.getCashReconciled()) || entry.getVoucherTime() == null) {
                    continue;
                }
                BigDecimal entryAmount = nullToZero(entry.getDebitAmount()).subtract(nullToZero(entry.getCreditAmount()));
                if (entryAmount.signum() == 0) {
                    continue;
                }
                for (FmsBankStatementDO statement : periodStatements) {
                    if (pairedStatementIds.contains(statement.getId()) || Boolean.TRUE.equals(statement.getReconciled())) {
                        continue;
                    }
                    BigDecimal stmtAmount = nullToZero(statement.getDebitAmount()).subtract(nullToZero(statement.getCreditAmount()));
                    if (stmtAmount.compareTo(entryAmount) != 0) {
                        continue;
                    }
                    long days = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(
                            entry.getVoucherTime().toLocalDate(), statement.getStatementDate()));
                    if (days > MATCH_DAYS) {
                        continue;
                    }
                    pairedEntryIds.add(entry.getId());
                    pairedStatementIds.add(statement.getId());
                    FmsUnreconciledSamePeriodItemRespVO item = new FmsUnreconciledSamePeriodItemRespVO();
                    item.setEntryId(entry.getId());
                    item.setStatementId(statement.getId());
                    item.setBookDate(entry.getVoucherTime().toLocalDate());
                    item.setStatementDate(statement.getStatementDate());
                    item.setSummary(entry.getDigest());
                    item.setAmount(entryAmount);
                    samePeriodItems.add(item);
                    break;
                }
            }
        }

        // 4. 未达账项统计（保留符号：红字冲销分录会减少未达支出；已配对的两端跳过）
        BigDecimal depositInTransit = BigDecimal.ZERO;   // 日记账有、银行无（收入）
        BigDecimal outstandingCheck = BigDecimal.ZERO;   // 日记账有、银行无（支出）
        BigDecimal bankCollection = BigDecimal.ZERO;     // 银行有、日记账无（收入）
        BigDecimal bankPayment = BigDecimal.ZERO;        // 银行有、日记账无（支出）
        for (FmsVoucherEntryDO entry : entries) {
            if (Boolean.TRUE.equals(entry.getCashReconciled()) || pairedEntryIds.contains(entry.getId())) {
                continue;
            }
            depositInTransit = depositInTransit.add(nullToZero(entry.getDebitAmount()));
            outstandingCheck = outstandingCheck.add(nullToZero(entry.getCreditAmount()));
        }
        for (FmsBankStatementDO statement : periodStatements) {
            if (Boolean.TRUE.equals(statement.getReconciled()) || pairedStatementIds.contains(statement.getId())) {
                continue;
            }
            if (nullToZero(statement.getDebitAmount()).signum() > 0) {
                bankCollection = bankCollection.add(statement.getDebitAmount());
            }
            if (nullToZero(statement.getCreditAmount()).signum() > 0) {
                bankPayment = bankPayment.add(statement.getCreditAmount());
            }
        }

        // 5. 调节后余额：账面 + 银行已收账面未收 − 银行已付账面未付 ＝ 银行 + 账面已收银行未收 − 账面已付银行未付
        BigDecimal adjustedBook = bookBalance.add(bankCollection).subtract(bankPayment);
        BigDecimal adjustedBank = bankBalance.add(depositInTransit).subtract(outstandingCheck);
        boolean balanced = adjustedBook.compareTo(adjustedBank) == 0;

        String warning = samePeriodItems.isEmpty() ? null
                : "当前期间已结账，存在 " + samePeriodItems.size()
                + " 笔两边都有但未勾对的项，已从未达项中排除并单独列示，可能导致金额重复，请人工核对";

        // 6. 保存或更新调节表
        FmsBankReconcileDO reconcile = bankReconcileMapper.selectByAccountSetIdAndSubjectIdAndMonth(
                accountSetId, subjectId, month);
        FmsBankReconcileDO saved = new FmsBankReconcileDO()
                .setAccountSetId(accountSetId).setSubjectId(subjectId).setMonth(month)
                .setBookBalance(bookBalance).setBankBalance(bankBalance)
                .setBankPlusDepositInTransit(depositInTransit).setBankLessOutstandingCheck(outstandingCheck)
                .setBookPlusBankCollection(bankCollection).setBookLessBankPayment(bankPayment)
                .setAdjustedBalance(adjustedBook).setStatus(balanced ? 1 : 0);
        if (reconcile == null) {
            bankReconcileMapper.insert(saved);
        } else {
            bankReconcileMapper.updateById(saved.setId(reconcile.getId()));
        }

        FmsBankReconcileRespVO respVO = new FmsBankReconcileRespVO();
        respVO.setBookBalance(bookBalance);
        respVO.setBankBalance(bankBalance);
        respVO.setBankPlusDepositInTransit(depositInTransit);
        respVO.setBankLessOutstandingCheck(outstandingCheck);
        respVO.setBookPlusBankCollection(bankCollection);
        respVO.setBookLessBankPayment(bankPayment);
        respVO.setAdjustedBalance(adjustedBook);
        respVO.setBalanced(balanced);
        respVO.setUnreconciledSamePeriodItems(samePeriodItems);
        respVO.setWarning(warning);
        return respVO;
    }

}
