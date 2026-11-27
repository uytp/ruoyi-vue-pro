package cn.iocoder.yudao.module.fms.service.settle;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.FmsLedgerEntryVO;
import cn.iocoder.yudao.module.fms.controller.admin.settle.vo.*;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAccountSetDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryTypeDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsInitialBalanceDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.settle.FmsSettleRecordDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherEntryDO;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsInitialBalanceMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.ledger.FmsLedgerQueryMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.settle.FmsSettleRecordMapper;
import cn.iocoder.yudao.module.fms.enums.common.FmsDebitCreditDirectionEnum;
import cn.iocoder.yudao.module.fms.service.config.FmsAccountSetService;
import cn.iocoder.yudao.module.fms.service.config.FmsAuxiliaryItemService;
import cn.iocoder.yudao.module.fms.service.config.FmsAuxiliaryTypeService;
import cn.iocoder.yudao.module.fms.service.config.FmsSubjectService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.number.NumberUtils.zeroIfNull;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;

/**
 * FMS 往来核销与账龄 Service 实现
 *
 * 思路：把某个往来单位（一个客户 / 供应商 / 职员）在某个往来科目下的所有凭证分录和期初余额，
 * 按方向拆成“借方项目”和“贷方项目”；已核销的金额记在 fms_settle_record 里，
 * 项目原始金额减去已核销金额就是未核销余额。账龄就是按未核销余额的凭证日期分组。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FmsSettleServiceImpl implements FmsSettleService {

    /** 可以做往来核销的辅助类别：1 客户、2 供应商、3 职员 */
    private static final Set<Integer> SETTLE_TYPES = Set.of(1, 2, 3);
    private static final String REF_ENTRY = "E";
    private static final String REF_OPENING = "O";
    /** 查全部分录时用的截止时间 */
    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(2999, 1, 1, 0, 0);

    @Resource
    private FmsSettleRecordMapper settleRecordMapper;
    @Resource
    private FmsLedgerQueryMapper ledgerQueryMapper;
    @Resource
    private FmsInitialBalanceMapper initialBalanceMapper;
    @Resource
    private FmsAccountSetService accountSetService;
    @Resource
    private FmsSubjectService subjectService;
    @Resource
    private FmsAuxiliaryTypeService auxiliaryTypeService;
    @Resource
    private FmsAuxiliaryItemService auxiliaryItemService;

    // ==================== 往来科目 ====================

    @Override
    public List<FmsSettleSubjectRespVO> getSettleSubjectList(Long accountSetId, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        Set<Long> settleTypeIds = new HashSet<>();
        for (FmsAuxiliaryTypeDO type : auxiliaryTypeService.getAuxiliaryTypeList(accountSetId, userId)) {
            if (SETTLE_TYPES.contains(type.getType())) {
                settleTypeIds.add(type.getId());
            }
        }
        List<FmsSettleSubjectRespVO> result = new ArrayList<>();
        for (FmsSubjectDO subject : subjectService.getSubjectList(accountSetId, null, userId)) {
            List<Long> ids = new ArrayList<>();
            for (Long typeId : CollUtil.emptyIfNull(subject.getAuxiliaryTypeIds())) {
                if (settleTypeIds.contains(typeId)) {
                    ids.add(typeId);
                }
            }
            if (ids.isEmpty()) {
                continue;
            }
            FmsSettleSubjectRespVO vo = new FmsSettleSubjectRespVO();
            vo.setId(subject.getId());
            vo.setCode(subject.getCode());
            vo.setName(subject.getName());
            vo.setBalanceDirection(subject.getBalanceDirection());
            vo.setAuxiliaryTypeIds(ids);
            result.add(vo);
        }
        result.sort(Comparator.comparing(FmsSettleSubjectRespVO::getCode));
        return result;
    }

    // ==================== 未核销项目 ====================

    @Override
    public FmsSettleOpenItemsRespVO getOpenItems(FmsSettleItemQueryReqVO reqVO, Long userId) {
        Context ctx = buildContext(reqVO.getAccountSetId(), reqVO.getSubjectId(), reqVO.getAuxiliaryTypeId(),
                userId, false);
        validateItem(ctx, reqVO.getAuxiliaryItemId(), userId);
        // 传了截止月份，就按那个月最后一天的状态来看（账龄分析里点开明细时用）
        LocalDate asOfDate = null;
        LocalDateTime endTime = FAR_FUTURE;
        if (reqVO.getMonth() != null && !reqVO.getMonth().isBlank()) {
            try {
                asOfDate = YearMonth.parse(reqVO.getMonth()).atEndOfMonth();
            } catch (DateTimeParseException e) {
                throw exception(SETTLE_MONTH_INVALID);
            }
            endTime = asOfDate.plusDays(1).atStartOfDay();
        }
        List<FmsSettleCalculator.Item> items = buildItemMap(ctx, Set.of(reqVO.getAuxiliaryItemId()),
                endTime, asOfDate).getOrDefault(reqVO.getAuxiliaryItemId(), Collections.emptyList());
        LocalDate ageDate = asOfDate != null ? asOfDate : LocalDate.now();
        FmsSettleOpenItemsRespVO resp = new FmsSettleOpenItemsRespVO();
        for (FmsSettleCalculator.Item item : items) {
            if (item.remain.signum() <= 0) {
                continue;
            }
            FmsSettleOpenItemRespVO vo = toOpenItemVO(item, ageDate);
            if (item.debit) {
                resp.getDebitItems().add(vo);
                resp.setDebitRemain(resp.getDebitRemain().add(item.remain));
            } else {
                resp.getCreditItems().add(vo);
                resp.setCreditRemain(resp.getCreditRemain().add(item.remain));
            }
        }
        return resp;
    }

    // ==================== 核销 / 撤销 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSettle(FmsSettleCreateReqVO reqVO, Long userId) {
        Context ctx = buildContext(reqVO.getAccountSetId(), reqVO.getSubjectId(), reqVO.getAuxiliaryTypeId(),
                userId, true);
        accountSetService.lockAccountSet(reqVO.getAccountSetId());
        validateItem(ctx, reqVO.getAuxiliaryItemId(), userId);

        // 1. 取当前的未核销状态，逐行校验
        Map<String, FmsSettleCalculator.Item> itemMap = convertMap(buildItemMap(ctx,
                Set.of(reqVO.getAuxiliaryItemId()), FAR_FUTURE, null)
                .getOrDefault(reqVO.getAuxiliaryItemId(), Collections.emptyList()),
                item -> item.ref);
        List<FmsSettleCalculator.Line> debits = validateLines(reqVO.getDebitItems(), true, itemMap);
        List<FmsSettleCalculator.Line> credits = validateLines(reqVO.getCreditItems(), false, itemMap);

        // 2. 借贷合计必须相等
        BigDecimal debitTotal = sumLines(debits);
        BigDecimal creditTotal = sumLines(credits);
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw exception(SETTLE_AMOUNT_NOT_EQUAL, formatAmount(debitTotal), formatAmount(creditTotal));
        }

        // 3. 核销日期：不填就取“今天”和“所选凭证最晚日期”中较晚的一个；手填的不能早于所选凭证
        LocalDate latestDate = latestItemDate(itemMap, debits, credits);
        LocalDate settleDate = reqVO.getSettleDate();
        if (settleDate != null && settleDate.isBefore(latestDate)) {
            throw exception(SETTLE_DATE_INVALID, latestDate.toString());
        }
        if (settleDate == null) {
            settleDate = laterDate(LocalDate.now(), latestDate);
        }

        // 4. 配对并保存，同一次核销共用一个批次号
        return saveBatch(ctx, reqVO.getAuxiliaryItemId(), FmsSettleCalculator.allocate(debits, credits),
                settleDate, reqVO.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer autoSettle(FmsSettleAutoReqVO reqVO, Long userId) {
        Context ctx = buildContext(reqVO.getAccountSetId(), reqVO.getSubjectId(), reqVO.getAuxiliaryTypeId(),
                userId, true);
        accountSetService.lockAccountSet(reqVO.getAccountSetId());
        Set<Long> itemIds = null;
        if (reqVO.getAuxiliaryItemId() != null) {
            validateItem(ctx, reqVO.getAuxiliaryItemId(), userId);
            itemIds = Set.of(reqVO.getAuxiliaryItemId());
        }
        int count = 0;
        for (Map.Entry<Long, List<FmsSettleCalculator.Item>> entry
                : buildItemMap(ctx, itemIds, FAR_FUTURE, null).entrySet()) {
            List<FmsSettleCalculator.Item> debits = new ArrayList<>();
            List<FmsSettleCalculator.Item> credits = new ArrayList<>();
            for (FmsSettleCalculator.Item item : entry.getValue()) {
                if (item.remain.signum() > 0) {
                    (item.debit ? debits : credits).add(item);
                }
            }
            Map<String, FmsSettleCalculator.Item> byRef = convertMap(entry.getValue(), item -> item.ref);
            // 每一对单独成一个批次，撤销时可以只撤销其中一对
            for (FmsSettleCalculator.Pair pair : FmsSettleCalculator.autoMatch(debits, credits)) {
                LocalDate settleDate = laterDate(LocalDate.now(),
                        laterDate(byRef.get(pair.debitRef).date, byRef.get(pair.creditRef).date));
                saveBatch(ctx, entry.getKey(), List.of(pair), settleDate, "自动核销");
                count++;
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeSettle(Long accountSetId, Long batchId, Long userId) {
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        if (settleRecordMapper.selectListByBatchId(accountSetId, batchId).isEmpty()) {
            throw exception(SETTLE_BATCH_NOT_EXISTS);
        }
        settleRecordMapper.deleteByBatchId(accountSetId, batchId);
    }

    @Override
    public List<FmsSettleBatchRespVO> getSettleBatchList(FmsSettleItemQueryReqVO reqVO, Long userId) {
        Context ctx = buildContext(reqVO.getAccountSetId(), reqVO.getSubjectId(), reqVO.getAuxiliaryTypeId(),
                userId, false);
        validateItem(ctx, reqVO.getAuxiliaryItemId(), userId);
        Map<String, FmsSettleCalculator.Item> itemMap = convertMap(buildItemMap(ctx,
                Set.of(reqVO.getAuxiliaryItemId()), FAR_FUTURE, null)
                .getOrDefault(reqVO.getAuxiliaryItemId(), Collections.emptyList()), item -> item.ref);

        // 按批次分组
        Map<Long, List<FmsSettleRecordDO>> batches = new LinkedHashMap<>();
        for (FmsSettleRecordDO record : settleRecordMapper.selectListByItem(
                reqVO.getAccountSetId(), reqVO.getAuxiliaryTypeId(), reqVO.getAuxiliaryItemId())) {
            if (ctx.subjectIds.contains(record.getSubjectId())) {
                batches.computeIfAbsent(record.getBatchId(), key -> new ArrayList<>()).add(record);
            }
        }
        List<FmsSettleBatchRespVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<FmsSettleRecordDO>> entry : batches.entrySet()) {
            Map<String, BigDecimal> debitSum = new LinkedHashMap<>();
            Map<String, BigDecimal> creditSum = new LinkedHashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (FmsSettleRecordDO record : entry.getValue()) {
                debitSum.merge(record.getDebitRef(), record.getAmount(), BigDecimal::add);
                creditSum.merge(record.getCreditRef(), record.getAmount(), BigDecimal::add);
                total = total.add(record.getAmount());
            }
            FmsSettleRecordDO first = entry.getValue().get(0);
            FmsSettleBatchRespVO vo = new FmsSettleBatchRespVO();
            vo.setBatchId(entry.getKey());
            vo.setSettleDate(first.getSettleDate());
            vo.setAmount(total);
            vo.setDebitDesc(describeRefs(debitSum, itemMap));
            vo.setCreditDesc(describeRefs(creditSum, itemMap));
            vo.setRemark(first.getRemark());
            vo.setCreator(first.getCreator());
            vo.setCreateTime(first.getCreateTime());
            result.add(vo);
        }
        result.sort(Comparator.comparing(FmsSettleBatchRespVO::getBatchId).reversed());
        return result;
    }

    // ==================== 账龄分析 ====================

    @Override
    public FmsSettleAgingRespVO getAging(FmsSettleAgingReqVO reqVO, Long userId) {
        Context ctx = buildContext(reqVO.getAccountSetId(), reqVO.getSubjectId(), reqVO.getAuxiliaryTypeId(),
                userId, false);
        LocalDate asOfDate;
        try {
            asOfDate = YearMonth.parse(reqVO.getMonth()).atEndOfMonth();
        } catch (DateTimeParseException e) {
            throw exception(SETTLE_MONTH_INVALID);
        }
        List<Integer> bounds = FmsSettleCalculator.parseBuckets(reqVO.getBuckets());
        boolean mainDebit = ObjUtil.equal(ctx.subject.getBalanceDirection(),
                FmsDebitCreditDirectionEnum.DEBIT.getType());

        FmsSettleAgingRespVO resp = new FmsSettleAgingRespVO();
        resp.setAsOfDate(asOfDate);
        resp.setBalanceDirection(ctx.subject.getBalanceDirection());
        resp.setBucketLabels(FmsSettleCalculator.bucketLabels(bounds));
        FmsSettleAgingRespVO.Row total = newAgingRow(bounds.size() + 1);
        total.setName("合计");

        Map<Long, List<FmsSettleCalculator.Item>> itemMap = buildItemMap(ctx, null,
                asOfDate.plusDays(1).atStartOfDay(), asOfDate);
        List<FmsAuxiliaryItemDO> auxiliaryItems = new ArrayList<>(
                auxiliaryItemService.getAuxiliaryItemList(reqVO.getAccountSetId(), reqVO.getAuxiliaryTypeId(), userId));
        auxiliaryItems.sort(Comparator.comparing(FmsAuxiliaryItemDO::getCode));
        for (FmsAuxiliaryItemDO auxiliaryItem : auxiliaryItems) {
            FmsSettleAgingRespVO.Row row = newAgingRow(bounds.size() + 1);
            row.setAuxiliaryItemId(auxiliaryItem.getId());
            row.setCode(auxiliaryItem.getCode());
            row.setName(auxiliaryItem.getName());
            for (FmsSettleCalculator.Item item : itemMap.getOrDefault(auxiliaryItem.getId(),
                    Collections.emptyList())) {
                if (item.remain.signum() <= 0) {
                    continue;
                }
                if (item.debit == mainDebit) {
                    int index = FmsSettleCalculator.bucketIndex(
                            FmsSettleCalculator.ageDays(item.date, asOfDate), bounds);
                    row.getBuckets().set(index, row.getBuckets().get(index).add(item.remain));
                    row.setMainTotal(row.getMainTotal().add(item.remain));
                } else {
                    row.setOppositeTotal(row.getOppositeTotal().add(item.remain));
                }
            }
            if (row.getMainTotal().signum() == 0 && row.getOppositeTotal().signum() == 0) {
                continue;
            }
            row.setNetBalance(row.getMainTotal().subtract(row.getOppositeTotal()));
            resp.getRows().add(row);
            total.setMainTotal(total.getMainTotal().add(row.getMainTotal()));
            total.setOppositeTotal(total.getOppositeTotal().add(row.getOppositeTotal()));
            for (int i = 0; i < row.getBuckets().size(); i++) {
                total.getBuckets().set(i, total.getBuckets().get(i).add(row.getBuckets().get(i)));
            }
        }
        total.setNetBalance(total.getMainTotal().subtract(total.getOppositeTotal()));
        resp.setTotal(total);
        return resp;
    }

    // ==================== 给凭证模块用 ====================

    @Override
    public void validateEntriesNotSettled(Long accountSetId, Collection<Long> entryIds) {
        if (CollUtil.isEmpty(entryIds)) {
            return;
        }
        List<String> refs = new ArrayList<>();
        for (Long entryId : entryIds) {
            refs.add(REF_ENTRY + entryId);
        }
        if (settleRecordMapper.selectCountByRefs(accountSetId, refs) > 0) {
            throw exception(SETTLE_ENTRY_SETTLED);
        }
    }

    // ==================== 内部方法 ====================

    private Context buildContext(Long accountSetId, Long subjectId, Long auxiliaryTypeId, Long userId, boolean write) {
        Context ctx = new Context();
        ctx.accountSet = write ? accountSetService.validateAccountSetWritePermission(accountSetId, userId)
                : accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        ctx.auxTypeId = auxiliaryTypeId;
        FmsAuxiliaryTypeDO type = auxiliaryTypeService.getAuxiliaryType(accountSetId, auxiliaryTypeId, userId);
        if (type == null || !SETTLE_TYPES.contains(type.getType())) {
            throw exception(SETTLE_AUX_TYPE_INVALID);
        }
        List<FmsSubjectDO> subjects = subjectService.getSubjectList(accountSetId, null, userId);
        ctx.subjectMap = convertMap(subjects, FmsSubjectDO::getId);
        ctx.subject = ctx.subjectMap.get(subjectId);
        if (ctx.subject == null || CollUtil.emptyIfNull(ctx.subject.getAuxiliaryTypeIds()).stream()
                .noneMatch(id -> ObjUtil.equal(id, auxiliaryTypeId))) {
            throw exception(SETTLE_SUBJECT_INVALID, ctx.subject == null ? String.valueOf(subjectId) : ctx.subject.getName());
        }
        // 所选科目及其所有下级科目
        Map<Long, List<Long>> childMap = new HashMap<>();
        for (FmsSubjectDO subject : subjects) {
            childMap.computeIfAbsent(subject.getParentId(), key -> new ArrayList<>()).add(subject.getId());
        }
        ctx.subjectIds = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(subjectId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (ctx.subjectIds.add(current)) {
                queue.addAll(childMap.getOrDefault(current, Collections.emptyList()));
            }
        }
        return ctx;
    }

    private void validateItem(Context ctx, Long auxiliaryItemId, Long userId) {
        FmsAuxiliaryItemDO item = auxiliaryItemService.getAuxiliaryItem(ctx.accountSet.getId(), auxiliaryItemId, userId);
        if (item == null || !ObjUtil.equal(item.getAuxiliaryTypeId(), ctx.auxTypeId)) {
            throw exception(SETTLE_ITEM_NOT_EXISTS);
        }
    }

    /**
     * 构建往来项目：key 是往来单位（辅助项目编号），value 是它名下按日期排好序的借方、贷方项目。
     * 项目来自期初余额和凭证分录；已核销金额来自核销记录。
     *
     * @param itemIds 只要这些往来单位，null 表示全部
     * @param endTime 凭证截止时间（不含）
     * @param asOfDate 只统计这天以前（含）做的核销，null 表示全部核销
     */
    private Map<Long, List<FmsSettleCalculator.Item>> buildItemMap(Context ctx, Set<Long> itemIds,
            LocalDateTime endTime, LocalDate asOfDate) {
        Map<Long, List<FmsSettleCalculator.Item>> result = new HashMap<>();
        Long accountSetId = ctx.accountSet.getId();

        // 1. 期初余额：按科目自己的余额方向换算成借方或贷方
        LocalDate openingDate = ctx.accountSet.getStartTime().toLocalDate();
        for (FmsInitialBalanceDO initial : initialBalanceMapper.selectListByAccountSetId(accountSetId)) {
            FmsSubjectDO subject = ctx.subjectMap.get(initial.getSubjectId());
            if (subject == null || !ctx.subjectIds.contains(initial.getSubjectId())
                    || CollUtil.isEmpty(initial.getAssistBalances())) {
                continue;
            }
            boolean debitNormal = ObjUtil.equal(subject.getBalanceDirection(),
                    FmsDebitCreditDirectionEnum.DEBIT.getType());
            for (FmsInitialBalanceDO.AssistBalance assist : initial.getAssistBalances()) {
                BigDecimal opening = zeroIfNull(assist.getOpeningAmount());
                if (opening.signum() == 0) {
                    continue;
                }
                for (FmsInitialBalanceDO.AuxiliaryItem aux : CollUtil.emptyIfNull(assist.getAuxiliaries())) {
                    if (!ObjUtil.equal(aux.getTypeId(), ctx.auxTypeId) || aux.getItemId() == null
                            || (itemIds != null && !itemIds.contains(aux.getItemId()))) {
                        continue;
                    }
                    BigDecimal signed = debitNormal ? opening : opening.negate();
                    FmsSettleCalculator.Item item = new FmsSettleCalculator.Item();
                    item.ref = REF_OPENING + initial.getSubjectId() + "-" + assist.getAssistCombinationId();
                    item.opening = true;
                    item.date = openingDate;
                    item.digest = "期初余额";
                    item.debit = signed.signum() > 0;
                    item.amount = signed.abs();
                    result.computeIfAbsent(aux.getItemId(), key -> new ArrayList<>()).add(item);
                }
            }
        }

        // 2. 凭证分录：借减贷为正是借方项目，为负就是贷方项目（红字冲销也是这样自然对冲）
        for (FmsLedgerEntryVO entry : ledgerQueryMapper.selectEntryListBeforeTime(accountSetId, endTime)) {
            if (!ctx.subjectIds.contains(entry.getSubjectId())) {
                continue;
            }
            BigDecimal net = zeroIfNull(entry.getDebitAmount()).subtract(zeroIfNull(entry.getCreditAmount()));
            if (net.signum() == 0) {
                continue;
            }
            for (FmsVoucherEntryDO.AuxiliaryItem aux : CollUtil.emptyIfNull(entry.getAuxiliaries())) {
                if (!ObjUtil.equal(aux.getTypeId(), ctx.auxTypeId) || aux.getItemId() == null
                        || (itemIds != null && !itemIds.contains(aux.getItemId()))) {
                    continue;
                }
                FmsSettleCalculator.Item item = new FmsSettleCalculator.Item();
                item.ref = REF_ENTRY + entry.getEntryId();
                item.voucherId = entry.getVoucherId();
                item.voucherNo = (entry.getVoucherWordName() == null ? "" : entry.getVoucherWordName() + "-")
                        + entry.getVoucherNumber();
                item.date = entry.getVoucherTime().toLocalDate();
                item.digest = entry.getDigest();
                item.debit = net.signum() > 0;
                item.amount = net.abs();
                result.computeIfAbsent(aux.getItemId(), key -> new ArrayList<>()).add(item);
            }
        }

        // 3. 扣掉已核销的金额
        Long singleItemId = itemIds != null && itemIds.size() == 1 ? itemIds.iterator().next() : null;
        Map<String, BigDecimal> settledMap = new HashMap<>();
        for (FmsSettleRecordDO record : settleRecordMapper.selectListByItem(accountSetId, ctx.auxTypeId, singleItemId)) {
            if (asOfDate != null && record.getSettleDate().isAfter(asOfDate)) {
                continue;
            }
            settledMap.merge(record.getDebitRef(), record.getAmount(), BigDecimal::add);
            settledMap.merge(record.getCreditRef(), record.getAmount(), BigDecimal::add);
        }
        for (List<FmsSettleCalculator.Item> items : result.values()) {
            for (FmsSettleCalculator.Item item : items) {
                item.settled = settledMap.getOrDefault(item.ref, BigDecimal.ZERO);
                item.remain = item.amount.subtract(item.settled).max(BigDecimal.ZERO);
            }
            items.sort(Comparator.comparing((FmsSettleCalculator.Item item) -> item.date)
                    .thenComparing(item -> item.ref));
        }
        return result;
    }

    /**
     * 校验核销请求的一侧：每个来源都存在、方向对、金额大于 0 且不超过未核销余额；同一个来源重复选中会合并
     */
    private List<FmsSettleCalculator.Line> validateLines(List<FmsSettleCreateReqVO.ItemAmount> lines,
            boolean debit, Map<String, FmsSettleCalculator.Item> itemMap) {
        if (CollUtil.isEmpty(lines)) {
            throw exception(SETTLE_ITEM_EMPTY);
        }
        Map<String, BigDecimal> merged = new LinkedHashMap<>();
        for (FmsSettleCreateReqVO.ItemAmount line : lines) {
            BigDecimal amount = line.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) {
                throw exception(SETTLE_AMOUNT_INVALID);
            }
            merged.merge(line.getRef(), amount, BigDecimal::add);
        }
        List<FmsSettleCalculator.Line> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : merged.entrySet()) {
            FmsSettleCalculator.Item item = itemMap.get(entry.getKey());
            if (item == null || item.debit != debit || item.remain.signum() <= 0) {
                throw exception(SETTLE_REF_NOT_FOUND, entry.getKey());
            }
            if (entry.getValue().compareTo(item.remain) > 0) {
                throw exception(SETTLE_AMOUNT_EXCEED, describe(item), formatAmount(item.remain),
                        formatAmount(entry.getValue()));
            }
            result.add(new FmsSettleCalculator.Line(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private Long saveBatch(Context ctx, Long auxiliaryItemId, List<FmsSettleCalculator.Pair> pairs,
            LocalDate settleDate, String remark) {
        List<FmsSettleRecordDO> records = new ArrayList<>();
        for (FmsSettleCalculator.Pair pair : pairs) {
            FmsSettleRecordDO record = new FmsSettleRecordDO();
            record.setAccountSetId(ctx.accountSet.getId());
            record.setBatchId(0L);
            record.setSubjectId(ctx.subject.getId());
            record.setAuxiliaryTypeId(ctx.auxTypeId);
            record.setAuxiliaryItemId(auxiliaryItemId);
            record.setDebitRef(pair.debitRef);
            record.setCreditRef(pair.creditRef);
            record.setAmount(pair.amount);
            record.setSettleDate(settleDate);
            record.setRemark(remark);
            settleRecordMapper.insert(record);
            records.add(record);
        }
        // 批次号取这一批第一条记录的编号
        Long batchId = records.get(0).getId();
        for (FmsSettleRecordDO record : records) {
            FmsSettleRecordDO update = new FmsSettleRecordDO();
            update.setId(record.getId());
            update.setBatchId(batchId);
            settleRecordMapper.updateById(update);
        }
        return batchId;
    }

    private static LocalDate laterDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    /**
     * 本次核销选中的所有项目里，凭证日期最晚的那一天
     */
    private static LocalDate latestItemDate(Map<String, FmsSettleCalculator.Item> itemMap,
            List<FmsSettleCalculator.Line> debits, List<FmsSettleCalculator.Line> credits) {
        LocalDate latest = LocalDate.MIN;
        for (FmsSettleCalculator.Line line : debits) {
            latest = laterDate(latest, itemMap.get(line.ref).date);
        }
        for (FmsSettleCalculator.Line line : credits) {
            latest = laterDate(latest, itemMap.get(line.ref).date);
        }
        return latest;
    }

    private static BigDecimal sumLines(List<FmsSettleCalculator.Line> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (FmsSettleCalculator.Line line : lines) {
            total = total.add(line.amount);
        }
        return total;
    }

    private static FmsSettleOpenItemRespVO toOpenItemVO(FmsSettleCalculator.Item item, LocalDate asOfDate) {
        FmsSettleOpenItemRespVO vo = new FmsSettleOpenItemRespVO();
        vo.setRef(item.ref);
        vo.setOpening(item.opening);
        vo.setVoucherId(item.voucherId);
        vo.setVoucherNo(item.voucherNo);
        vo.setVoucherDate(item.date);
        vo.setDigest(item.digest);
        vo.setDirection(item.debit ? FmsDebitCreditDirectionEnum.DEBIT.getType()
                : FmsDebitCreditDirectionEnum.CREDIT.getType());
        vo.setAmount(item.amount);
        vo.setSettledAmount(item.settled);
        vo.setRemainAmount(item.remain);
        vo.setAgeDays(FmsSettleCalculator.ageDays(item.date, asOfDate));
        return vo;
    }

    private static FmsSettleAgingRespVO.Row newAgingRow(int bucketCount) {
        FmsSettleAgingRespVO.Row row = new FmsSettleAgingRespVO.Row();
        for (int i = 0; i < bucketCount; i++) {
            row.getBuckets().add(BigDecimal.ZERO);
        }
        return row;
    }

    private static String describe(FmsSettleCalculator.Item item) {
        return item.opening ? "期初余额" : item.voucherNo + " " + (item.digest == null ? "" : item.digest);
    }

    private static String describeRefs(Map<String, BigDecimal> sums, Map<String, FmsSettleCalculator.Item> itemMap) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sums.entrySet()) {
            FmsSettleCalculator.Item item = itemMap.get(entry.getKey());
            parts.add((item == null ? "（凭证已变更）" : describe(item)) + " 核销 " + formatAmount(entry.getValue()));
        }
        return String.join("；", parts);
    }

    private static String formatAmount(BigDecimal amount) {
        return new DecimalFormat("#,##0.00").format(amount);
    }

    /**
     * 一次查询用到的账套、科目信息
     */
    private static class Context {
        FmsAccountSetDO accountSet;
        FmsSubjectDO subject;
        Long auxTypeId;
        Map<Long, FmsSubjectDO> subjectMap;
        Set<Long> subjectIds;
    }

}
