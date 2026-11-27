package cn.iocoder.yudao.module.fms.service.asset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.fms.controller.admin.asset.vo.*;
import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.FmsLedgerListReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.subjectbalance.FmsLedgerSubjectBalanceRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherEntrySaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.asset.FmsAssetCategoryDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.asset.FmsAssetDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.asset.FmsAssetDepreciationDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAccountSetDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsVoucherWordDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherDO;
import cn.iocoder.yudao.module.fms.dal.mysql.asset.FmsAssetCategoryMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.asset.FmsAssetDepreciationMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.asset.FmsAssetMapper;
import cn.iocoder.yudao.module.fms.enums.voucher.FmsVoucherStatusEnum;
import cn.iocoder.yudao.module.fms.service.config.FmsAccountSetService;
import cn.iocoder.yudao.module.fms.service.config.FmsSubjectService;
import cn.iocoder.yudao.module.fms.service.config.FmsVoucherWordService;
import cn.iocoder.yudao.module.fms.service.ledger.FmsLedgerService;
import cn.iocoder.yudao.module.fms.service.voucher.FmsVoucherService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;

/**
 * FMS 固定资产 Service 实现
 *
 * 思路：资产卡片记录原值、净残值率、折旧月数；每个月“计提折旧”时，给每个在用资产算一笔折旧，
 * 按类别汇总成一张凭证（借 折旧费用，贷 累计折旧），并把明细记在 fms_asset_depreciation 里。
 * 累计折旧 = 进入系统前已提 + 本系统里各月折旧之和，净值 = 原值 - 累计折旧。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FmsAssetServiceImpl implements FmsAssetService {

    /** 固定资产清理科目编码 */
    private static final String CLEARING_CODE = "1606";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Resource
    private FmsAssetCategoryMapper categoryMapper;
    @Resource
    private FmsAssetMapper assetMapper;
    @Resource
    private FmsAssetDepreciationMapper depreciationMapper;
    @Resource
    private FmsAccountSetService accountSetService;
    @Resource
    private FmsSubjectService subjectService;
    @Resource
    private FmsVoucherService voucherService;
    @Resource
    private FmsVoucherWordService voucherWordService;
    @Resource
    private FmsLedgerService ledgerService;

    // ==================== 资产类别 ====================

    @Override
    public List<FmsAssetCategoryRespVO> getCategoryList(Long accountSetId, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        Map<Long, FmsSubjectDO> subjectMap = getSubjectMap(accountSetId, userId);
        List<FmsAssetCategoryRespVO> result = new ArrayList<>();
        for (FmsAssetCategoryDO category : categoryMapper.selectListByAccountSetId(accountSetId)) {
            FmsAssetCategoryRespVO vo = new FmsAssetCategoryRespVO();
            vo.setId(category.getId());
            vo.setCode(category.getCode());
            vo.setName(category.getName());
            vo.setLifeMonths(category.getLifeMonths());
            vo.setResidualRate(category.getResidualRate());
            vo.setAssetSubjectId(category.getAssetSubjectId());
            vo.setAssetSubjectName(subjectLabel(subjectMap.get(category.getAssetSubjectId())));
            vo.setAccumSubjectId(category.getAccumSubjectId());
            vo.setAccumSubjectName(subjectLabel(subjectMap.get(category.getAccumSubjectId())));
            vo.setExpenseSubjectId(category.getExpenseSubjectId());
            vo.setExpenseSubjectName(subjectLabel(subjectMap.get(category.getExpenseSubjectId())));
            vo.setRemark(category.getRemark());
            vo.setAssetCount(assetMapper.selectCountByCategoryId(accountSetId, category.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCategory(FmsAssetCategorySaveReqVO reqVO, Long userId) {
        accountSetService.validateAccountSetWritePermission(reqVO.getAccountSetId(), userId);
        accountSetService.lockAccountSet(reqVO.getAccountSetId());
        Long accountSetId = reqVO.getAccountSetId();
        // 1. 编码不能重复
        FmsAssetCategoryDO same = categoryMapper.selectByAccountSetIdAndCode(accountSetId, reqVO.getCode());
        if (same != null && !ObjUtil.equal(same.getId(), reqVO.getId())) {
            throw exception(ASSET_CATEGORY_CODE_DUPLICATE, reqVO.getCode());
        }
        // 2. 三个科目必须存在并且是末级科目
        Map<Long, FmsSubjectDO> subjectMap = getSubjectMap(accountSetId, userId);
        Set<Long> parentIds = new HashSet<>();
        subjectMap.values().forEach(s -> parentIds.add(s.getParentId()));
        validateLeafSubject(subjectMap, parentIds, reqVO.getAssetSubjectId(), "固定资产科目");
        validateLeafSubject(subjectMap, parentIds, reqVO.getAccumSubjectId(), "累计折旧科目");
        validateLeafSubject(subjectMap, parentIds, reqVO.getExpenseSubjectId(), "折旧费用科目");

        FmsAssetCategoryDO category = new FmsAssetCategoryDO();
        category.setAccountSetId(accountSetId);
        category.setCode(reqVO.getCode().trim());
        category.setName(reqVO.getName().trim());
        category.setLifeMonths(reqVO.getLifeMonths());
        category.setResidualRate(reqVO.getResidualRate());
        category.setAssetSubjectId(reqVO.getAssetSubjectId());
        category.setAccumSubjectId(reqVO.getAccumSubjectId());
        category.setExpenseSubjectId(reqVO.getExpenseSubjectId());
        category.setRemark(reqVO.getRemark());
        if (reqVO.getId() == null) {
            categoryMapper.insert(category);
        } else {
            validateCategory(accountSetId, reqVO.getId());
            category.setId(reqVO.getId());
            categoryMapper.updateById(category);
        }
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long accountSetId, Long id, Long userId) {
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        FmsAssetCategoryDO category = validateCategory(accountSetId, id);
        if (assetMapper.selectCountByCategoryId(accountSetId, id) > 0) {
            throw exception(ASSET_CATEGORY_USED, category.getName());
        }
        categoryMapper.deleteById(id);
    }

    // ==================== 资产卡片 ====================

    @Override
    public List<FmsAssetRespVO> getAssetList(Long accountSetId, Long categoryId, Integer status, String keyword,
            Long userId) {
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        YearMonth accountStart = YearMonth.from(accountSet.getStartTime());
        Map<Long, FmsAssetCategoryDO> categoryMap = convertMap(categoryMapper.selectListByAccountSetId(accountSetId),
                FmsAssetCategoryDO::getId);
        Map<Long, List<FmsAssetDepreciationDO>> recordMap = groupByAsset(loadRecords(accountSetId, userId, false));
        List<FmsAssetRespVO> result = new ArrayList<>();
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        for (FmsAssetDO asset : assetMapper.selectListByAccountSetId(accountSetId)) {
            if (categoryId != null && !ObjUtil.equal(asset.getCategoryId(), categoryId)) {
                continue;
            }
            if (status != null && !ObjUtil.equal(asset.getStatus(), status)) {
                continue;
            }
            if (!key.isEmpty() && !(asset.getCode() + " " + asset.getName()).toLowerCase().contains(key)) {
                continue;
            }
            result.add(toResp(asset, categoryMap.get(asset.getCategoryId()),
                    recordMap.getOrDefault(asset.getId(), Collections.emptyList()), accountStart));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAsset(FmsAssetSaveReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        FmsAssetCategoryDO category = validateCategory(accountSetId, reqVO.getCategoryId());

        // 1. 取默认值并校验数字
        LocalDate startDate = reqVO.getStartDate() != null ? reqVO.getStartDate() : reqVO.getPurchaseDate();
        BigDecimal quantity = reqVO.getQuantity() != null ? reqVO.getQuantity() : BigDecimal.ONE;
        BigDecimal rate = reqVO.getResidualRate() != null ? reqVO.getResidualRate() : category.getResidualRate();
        int life = reqVO.getLifeMonths() != null ? reqVO.getLifeMonths() : category.getLifeMonths();
        int initMonths = reqVO.getInitDeprMonths() != null ? reqVO.getInitDeprMonths() : 0;
        BigDecimal initAccum = reqVO.getInitAccumDepr() != null ? reqVO.getInitAccumDepr() : ZERO;
        BigDecimal original = reqVO.getOriginalValue();
        if (original == null || original.signum() <= 0 || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0
                || life <= 0) {
            throw exception(ASSET_VALUE_INVALID);
        }
        original = original.setScale(2, java.math.RoundingMode.HALF_UP);
        initAccum = initAccum.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal residual = FmsAssetCalculator.residualValue(original, rate);
        if (initMonths > life || initAccum.compareTo(original.subtract(residual)) > 0) {
            throw exception(ASSET_INIT_INVALID);
        }

        // 2. 编号：不填就自动生成，填了不能重复
        String code = reqVO.getCode() == null ? "" : reqVO.getCode().trim();
        FmsAssetDO existing = reqVO.getId() == null ? null : validateAsset(accountSetId, reqVO.getId());
        if (code.isEmpty()) {
            code = existing != null ? existing.getCode() : nextAssetCode(accountSetId);
        }
        FmsAssetDO same = assetMapper.selectByAccountSetIdAndCode(accountSetId, code);
        if (same != null && !ObjUtil.equal(same.getId(), reqVO.getId())) {
            throw exception(ASSET_CODE_DUPLICATE, code);
        }

        // 3. 已处置的不能改；计提过折旧的，关键信息不能改
        boolean hasRecords = false;
        if (existing != null) {
            if (ObjUtil.equal(existing.getStatus(), FmsAssetDO.STATUS_DISPOSED)) {
                throw exception(ASSET_DISPOSED, existing.getName());
            }
            hasRecords = !getValidRecordsOfAsset(accountSetId, existing.getId(), userId).isEmpty();
            if (hasRecords && !(ObjUtil.equal(existing.getCategoryId(), reqVO.getCategoryId())
                    && ObjUtil.equal(existing.getStartDate(), startDate)
                    && existing.getOriginalValue().compareTo(original) == 0
                    && existing.getResidualRate().compareTo(rate) == 0
                    && ObjUtil.equal(existing.getLifeMonths(), life)
                    && ObjUtil.equal(existing.getInitDeprMonths(), initMonths)
                    && existing.getInitAccumDepr().compareTo(initAccum) == 0)) {
                throw exception(ASSET_HAS_DEPRECIATION, existing.getName());
            }
        }

        // 4. 新资产的开始折旧月份，不能早于已经计提过折旧的月份（否则中间会缺一段）
        if (!hasRecords) {
            YearMonth accountStart = YearMonth.from(accountSet.getStartTime());
            YearMonth first = YearMonth.from(startDate).plusMonths(1);
            if (first.isBefore(accountStart)) {
                first = accountStart;
            }
            String latest = latestPeriod(loadRecords(accountSetId, userId, false));
            if (latest != null && !first.isAfter(YearMonth.parse(latest))) {
                throw exception(ASSET_START_TOO_EARLY, first.toString(), latest, first.toString());
            }
        }

        FmsAssetDO asset = new FmsAssetDO();
        asset.setAccountSetId(accountSetId);
        asset.setCode(code);
        asset.setName(reqVO.getName().trim());
        asset.setCategoryId(reqVO.getCategoryId());
        asset.setSpec(reqVO.getSpec());
        asset.setUnit(reqVO.getUnit());
        asset.setQuantity(quantity);
        asset.setPurchaseDate(reqVO.getPurchaseDate());
        asset.setStartDate(startDate);
        asset.setOriginalValue(original);
        asset.setResidualRate(rate);
        asset.setResidualValue(residual);
        asset.setLifeMonths(life);
        asset.setInitDeprMonths(initMonths);
        asset.setInitAccumDepr(initAccum);
        asset.setRemark(reqVO.getRemark());
        if (existing == null) {
            asset.setStatus(FmsAssetDO.STATUS_IN_USE);
            assetMapper.insert(asset);
        } else {
            asset.setId(existing.getId());
            assetMapper.updateById(asset);
        }
        return asset.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAsset(Long accountSetId, Long id, Long userId) {
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        FmsAssetDO asset = validateAsset(accountSetId, id);
        if (ObjUtil.equal(asset.getStatus(), FmsAssetDO.STATUS_DISPOSED)) {
            throw exception(ASSET_DISPOSED, asset.getName());
        }
        loadRecords(accountSetId, userId, true); // 顺便清掉凭证已被删除的失效明细
        if (!getValidRecordsOfAsset(accountSetId, id, userId).isEmpty()) {
            throw exception(ASSET_DELETE_HAS_DEPRECIATION, asset.getName());
        }
        assetMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long disposeAsset(FmsAssetDisposeReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        FmsAssetDO asset = validateAsset(accountSetId, reqVO.getId());
        if (ObjUtil.equal(asset.getStatus(), FmsAssetDO.STATUS_DISPOSED)) {
            throw exception(ASSET_DISPOSED, asset.getName());
        }
        if (reqVO.getDisposeDate().isBefore(asset.getStartDate())) {
            throw exception(ASSET_DISPOSE_DATE_INVALID, asset.getStartDate().toString());
        }
        loadRecords(accountSetId, userId, true);
        List<FmsAssetDepreciationDO> records = getValidRecordsOfAsset(accountSetId, asset.getId(), userId);
        YearMonth disposeMonth = YearMonth.from(reqVO.getDisposeDate());

        // 1. 处置月份之后不能还有折旧记录；处置当月如果该提折旧，必须先提完
        for (FmsAssetDepreciationDO record : records) {
            if (YearMonth.parse(record.getPeriod()).isAfter(disposeMonth)) {
                throw exception(ASSET_DISPOSE_LATER_DEPR, disposeMonth.toString());
            }
        }
        boolean hasThisMonth = records.stream().anyMatch(r -> disposeMonth.toString().equals(r.getPeriod()));
        int depreciatedMonths = asset.getInitDeprMonths() + records.size();
        YearMonth accountStart = YearMonth.from(accountSet.getStartTime());
        if (!hasThisMonth && !disposeMonth.isBefore(accountStart)
                && FmsAssetCalculator.started(asset.getStartDate(), disposeMonth)
                && depreciatedMonths < asset.getLifeMonths()
                && asset.getInitAccumDepr().add(sum(records)).compareTo(
                        asset.getOriginalValue().subtract(asset.getResidualValue())) < 0) {
            throw exception(ASSET_DISPOSE_DEPR_MISSING, disposeMonth.toString());
        }

        // 2. 转入清理：借 累计折旧、固定资产清理（净值），贷 固定资产（原值）
        FmsAssetCategoryDO category = validateCategory(accountSetId, asset.getCategoryId());
        FmsSubjectDO clearing = findSubjectByCode(accountSetId, CLEARING_CODE, userId);
        BigDecimal accum = asset.getInitAccumDepr().add(sum(records));
        BigDecimal net = asset.getOriginalValue().subtract(accum);
        String remark = reqVO.getRemark() == null || reqVO.getRemark().isBlank() ? "" : "（" + reqVO.getRemark().trim() + "）";
        String digest = "处置固定资产-" + asset.getName() + remark;
        List<FmsVoucherEntrySaveReqVO> entries = new ArrayList<>();
        if (accum.signum() > 0) {
            entries.add(entry(digest, category.getAccumSubjectId(), accum, ZERO));
        }
        if (net.signum() > 0) {
            entries.add(entry(digest, clearing.getId(), net, ZERO));
        }
        entries.add(entry(digest, category.getAssetSubjectId(), ZERO, asset.getOriginalValue()));
        Long voucherId = createVoucher(accountSetId, reqVO.getDisposeDate().atStartOfDay(), entries, userId);

        FmsAssetDO update = new FmsAssetDO();
        update.setId(asset.getId());
        update.setStatus(FmsAssetDO.STATUS_DISPOSED);
        update.setDisposeDate(reqVO.getDisposeDate());
        update.setDisposeVoucherId(voucherId);
        assetMapper.updateById(update);
        return voucherId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeDispose(Long accountSetId, Long id, Long userId) {
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        FmsAssetDO asset = validateAsset(accountSetId, id);
        if (!ObjUtil.equal(asset.getStatus(), FmsAssetDO.STATUS_DISPOSED)) {
            throw exception(ASSET_NOT_DISPOSED, asset.getName());
        }
        deleteVoucherIfExists(accountSetId, asset.getDisposeVoucherId(), userId);
        assetMapper.update(null, new LambdaUpdateWrapper<FmsAssetDO>()
                .eq(FmsAssetDO::getId, id)
                .set(FmsAssetDO::getStatus, FmsAssetDO.STATUS_IN_USE)
                .set(FmsAssetDO::getDisposeDate, null)
                .set(FmsAssetDO::getDisposeVoucherId, null));
    }

    // ==================== 折旧 ====================

    @Override
    public FmsAssetDepreciationRespVO getDepreciation(Long accountSetId, String period, Long userId) {
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        YearMonth ym = parsePeriod(period);
        YearMonth accountStart = YearMonth.from(accountSet.getStartTime());
        if (ym.isBefore(accountStart)) {
            throw exception(ASSET_PERIOD_BEFORE_START, period, accountStart.toString());
        }
        List<FmsAssetDO> assets = assetMapper.selectListByAccountSetId(accountSetId);
        Map<Long, FmsAssetDO> assetMap = convertMap(assets, FmsAssetDO::getId);
        Map<Long, FmsAssetCategoryDO> categoryMap = convertMap(categoryMapper.selectListByAccountSetId(accountSetId),
                FmsAssetCategoryDO::getId);
        Map<Long, FmsSubjectDO> subjectMap = getSubjectMap(accountSetId, userId);
        List<FmsAssetDepreciationDO> records = loadRecords(accountSetId, userId, false);

        FmsAssetDepreciationRespVO resp = new FmsAssetDepreciationRespVO();
        resp.setPeriod(period);
        FmsAssetDepreciationRespVO.Row total = new FmsAssetDepreciationRespVO.Row();
        total.setName("合计");
        Map<Long, FmsAssetDepreciationRespVO.CategoryRow> categoryRows = new LinkedHashMap<>();

        List<FmsAssetDepreciationDO> thisMonth = new ArrayList<>();
        for (FmsAssetDepreciationDO record : records) {
            if (period.equals(record.getPeriod())) {
                thisMonth.add(record);
            }
        }
        if (!thisMonth.isEmpty()) {
            // 已经计提：显示实际结果
            resp.setGenerated(true);
            resp.setVoucherId(thisMonth.get(0).getVoucherId());
            resp.setVoucherNo(getVoucherNo(accountSetId, resp.getVoucherId(), userId));
            for (FmsAssetDepreciationDO record : thisMonth) {
                FmsAssetDO asset = assetMap.get(record.getAssetId());
                if (asset == null) {
                    continue;
                }
                int months = asset.getInitDeprMonths() + (int) records.stream()
                        .filter(r -> ObjUtil.equal(r.getAssetId(), asset.getId()) && r.getPeriod().compareTo(period) <= 0)
                        .count();
                addRow(resp, total, categoryRows, asset, categoryMap.get(asset.getCategoryId()), subjectMap,
                        record.getAmount(), record.getAccumAfter(), record.getNetAfter(), months);
            }
        } else {
            // 还没计提：预览
            resp.setGenerated(false);
            Map<Long, List<FmsAssetDepreciationDO>> recordMap = groupByAsset(records);
            for (Calc calc : calcPeriod(ym, assets, recordMap)) {
                addRow(resp, total, categoryRows, calc.asset, categoryMap.get(calc.asset.getCategoryId()), subjectMap,
                        calc.amount, calc.accumAfter, calc.netAfter, calc.monthsAfter);
            }
            if (!ym.minusMonths(1).isBefore(accountStart) && !calcPeriod(ym.minusMonths(1), assets, recordMap).isEmpty()) {
                resp.getWarnings().add("上个月（" + ym.minusMonths(1) + "）的折旧还没有计提，请先计提上个月，再计提这个月");
            }
        }
        resp.setTotal(total);
        resp.setCategories(new ArrayList<>(categoryRows.values()));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateDepreciation(FmsAssetDepreciationReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        YearMonth ym = parsePeriod(reqVO.getPeriod());
        YearMonth accountStart = YearMonth.from(accountSet.getStartTime());
        if (ym.isBefore(accountStart)) {
            throw exception(ASSET_PERIOD_BEFORE_START, reqVO.getPeriod(), accountStart.toString());
        }
        List<FmsAssetDepreciationDO> records = loadRecords(accountSetId, userId, true);
        for (FmsAssetDepreciationDO record : records) {
            if (reqVO.getPeriod().equals(record.getPeriod())) {
                throw exception(ASSET_DEPRECIATION_GENERATED, reqVO.getPeriod());
            }
        }
        List<FmsAssetDO> assets = assetMapper.selectListByAccountSetId(accountSetId);
        Map<Long, List<FmsAssetDepreciationDO>> recordMap = groupByAsset(records);
        // 折旧要一个月一个月往后做：上个月该提的没提，不让提这个月
        if (!ym.minusMonths(1).isBefore(accountStart) && !calcPeriod(ym.minusMonths(1), assets, recordMap).isEmpty()) {
            throw exception(ASSET_DEPRECIATION_PREV_MISSING, ym.minusMonths(1).toString());
        }
        List<Calc> calcs = calcPeriod(ym, assets, recordMap);
        if (calcs.isEmpty()) {
            throw exception(ASSET_DEPRECIATION_NOTHING, reqVO.getPeriod());
        }

        // 按类别汇总：每个类别一借一贷
        Map<Long, FmsAssetCategoryDO> categoryMap = convertMap(categoryMapper.selectListByAccountSetId(accountSetId),
                FmsAssetCategoryDO::getId);
        Map<Long, BigDecimal> categoryTotal = new TreeMap<>();
        for (Calc calc : calcs) {
            categoryTotal.merge(calc.asset.getCategoryId(), calc.amount, BigDecimal::add);
        }
        List<FmsVoucherEntrySaveReqVO> entries = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : categoryTotal.entrySet()) {
            FmsAssetCategoryDO category = categoryMap.get(entry.getKey());
            if (category == null) {
                throw exception(ASSET_CATEGORY_NOT_EXISTS);
            }
            if (category.getExpenseSubjectId() == null || category.getAccumSubjectId() == null) {
                throw exception(ASSET_DEPRECIATION_NO_EXPENSE, category.getName());
            }
            String digest = "计提" + reqVO.getPeriod() + "折旧-" + category.getName();
            entries.add(entry(digest, category.getExpenseSubjectId(), entry.getValue(), ZERO));
            entries.add(entry(digest, category.getAccumSubjectId(), ZERO, entry.getValue()));
        }
        Long voucherId = createVoucher(accountSetId, ym.atEndOfMonth().atStartOfDay(), entries, userId);
        for (Calc calc : calcs) {
            FmsAssetDepreciationDO record = new FmsAssetDepreciationDO();
            record.setAccountSetId(accountSetId);
            record.setAssetId(calc.asset.getId());
            record.setPeriod(reqVO.getPeriod());
            record.setAmount(calc.amount);
            record.setAccumAfter(calc.accumAfter);
            record.setNetAfter(calc.netAfter);
            record.setVoucherId(voucherId);
            depreciationMapper.insert(record);
        }
        return voucherId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeDepreciation(FmsAssetDepreciationReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        parsePeriod(reqVO.getPeriod());
        List<FmsAssetDepreciationDO> records = loadRecords(accountSetId, userId, true);
        List<FmsAssetDepreciationDO> thisMonth = new ArrayList<>();
        for (FmsAssetDepreciationDO record : records) {
            if (reqVO.getPeriod().equals(record.getPeriod())) {
                thisMonth.add(record);
            }
        }
        if (thisMonth.isEmpty()) {
            throw exception(ASSET_DEPRECIATION_NOT_GENERATED, reqVO.getPeriod());
        }
        // 只能从最近计提的月份往前撤销
        String latest = latestPeriod(records);
        if (latest != null && latest.compareTo(reqVO.getPeriod()) > 0) {
            throw exception(ASSET_DEPRECIATION_REVOKE_NEWER, latest);
        }
        // 这个月提过折旧的资产如果已经处置，要先撤销处置
        Map<Long, FmsAssetDO> assetMap = convertMap(assetMapper.selectListByAccountSetId(accountSetId), FmsAssetDO::getId);
        for (FmsAssetDepreciationDO record : thisMonth) {
            FmsAssetDO asset = assetMap.get(record.getAssetId());
            if (asset != null && ObjUtil.equal(asset.getStatus(), FmsAssetDO.STATUS_DISPOSED)) {
                throw exception(ASSET_DEPRECIATION_REVOKE_DISPOSED, asset.getName());
            }
        }
        Set<Long> voucherIds = new LinkedHashSet<>();
        thisMonth.forEach(r -> voucherIds.add(r.getVoucherId()));
        for (Long voucherId : voucherIds) {
            deleteVoucherIfExists(accountSetId, voucherId, userId);
        }
        depreciationMapper.deleteByPeriod(accountSetId, reqVO.getPeriod());
    }

    // ==================== 对账 ====================

    @Override
    public FmsAssetReconcileRespVO getReconcile(Long accountSetId, String month, Long userId) {
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        YearMonth ym = parsePeriod(month);
        YearMonth accountStart = YearMonth.from(accountSet.getStartTime());
        if (ym.isBefore(accountStart)) {
            throw exception(ASSET_PERIOD_BEFORE_START, month, accountStart.toString());
        }
        Map<Long, FmsSubjectDO> subjectMap = getSubjectMap(accountSetId, userId);
        Map<Long, FmsAssetCategoryDO> categoryMap = convertMap(categoryMapper.selectListByAccountSetId(accountSetId),
                FmsAssetCategoryDO::getId);
        Map<Long, List<FmsAssetDepreciationDO>> recordMap = groupByAsset(loadRecords(accountSetId, userId, false));

        // 1. 卡片侧：月末还在的资产（已处置的，处置日在月末之后也算）
        Map<Long, BigDecimal> cardOriginal = new LinkedHashMap<>();
        Map<Long, BigDecimal> cardAccum = new LinkedHashMap<>();
        LocalDate monthEnd = ym.atEndOfMonth();
        for (FmsAssetDO asset : assetMapper.selectListByAccountSetId(accountSetId)) {
            FmsAssetCategoryDO category = categoryMap.get(asset.getCategoryId());
            if (category == null || YearMonth.from(asset.getPurchaseDate()).isAfter(ym)) {
                continue;
            }
            boolean gone = ObjUtil.equal(asset.getStatus(), FmsAssetDO.STATUS_DISPOSED)
                    && asset.getDisposeDate() != null && !asset.getDisposeDate().isAfter(monthEnd);
            if (gone) {
                continue;
            }
            BigDecimal accum = asset.getInitAccumDepr();
            for (FmsAssetDepreciationDO record : recordMap.getOrDefault(asset.getId(), Collections.emptyList())) {
                if (record.getPeriod().compareTo(month) <= 0) {
                    accum = accum.add(record.getAmount());
                }
            }
            cardOriginal.merge(category.getAssetSubjectId(), asset.getOriginalValue(), BigDecimal::add);
            cardAccum.merge(category.getAccumSubjectId(), accum, BigDecimal::add);
        }
        // 类别用到但没有卡片的科目也要对一下总账
        for (FmsAssetCategoryDO category : categoryMap.values()) {
            cardOriginal.putIfAbsent(category.getAssetSubjectId(), ZERO);
            cardAccum.putIfAbsent(category.getAccumSubjectId(), ZERO);
        }

        // 2. 总账侧：科目余额表里各科目的期末余额
        FmsLedgerListReqVO ledgerReq = new FmsLedgerListReqVO();
        ledgerReq.setAccountSetId(accountSetId);
        ledgerReq.setStartMonth(accountStart.toString());
        ledgerReq.setEndMonth(month);
        Map<Long, FmsLedgerSubjectBalanceRespVO> ledgerMap = new HashMap<>();
        flattenBalance(ledgerService.getSubjectBalanceList(ledgerReq, userId), ledgerMap);

        FmsAssetReconcileRespVO resp = new FmsAssetReconcileRespVO();
        resp.setMonth(month);
        boolean balanced = true;
        for (Map.Entry<Long, BigDecimal> entry : cardOriginal.entrySet()) {
            FmsLedgerSubjectBalanceRespVO node = ledgerMap.get(entry.getKey());
            BigDecimal ledger = node == null ? ZERO : nvl(node.getEndingDebitAmount()).subtract(nvl(node.getEndingCreditAmount()));
            balanced &= addReconcileRow(resp, subjectMap.get(entry.getKey()), 1, entry.getValue(), ledger);
        }
        for (Map.Entry<Long, BigDecimal> entry : cardAccum.entrySet()) {
            FmsLedgerSubjectBalanceRespVO node = ledgerMap.get(entry.getKey());
            BigDecimal ledger = node == null ? ZERO : nvl(node.getEndingCreditAmount()).subtract(nvl(node.getEndingDebitAmount()));
            balanced &= addReconcileRow(resp, subjectMap.get(entry.getKey()), 2, entry.getValue(), ledger);
        }
        resp.setBalanced(balanced);
        return resp;
    }

    // ==================== 内部方法 ====================

    /** 一个资产在某个月的计算结果 */
    private static class Calc {
        FmsAssetDO asset;
        BigDecimal amount;
        BigDecimal accumAfter;
        BigDecimal netAfter;
        int monthsAfter;
    }

    /**
     * 计算 ym 这个月每个在用资产该提多少折旧：启用月份早于 ym、这个月还没记录、还没提完的才算
     */
    private List<Calc> calcPeriod(YearMonth ym, List<FmsAssetDO> assets,
            Map<Long, List<FmsAssetDepreciationDO>> recordMap) {
        List<Calc> result = new ArrayList<>();
        for (FmsAssetDO asset : assets) {
            if (!ObjUtil.equal(asset.getStatus(), FmsAssetDO.STATUS_IN_USE)
                    || !FmsAssetCalculator.started(asset.getStartDate(), ym)) {
                continue;
            }
            List<FmsAssetDepreciationDO> own = recordMap.getOrDefault(asset.getId(), Collections.emptyList());
            if (own.stream().anyMatch(r -> ym.toString().equals(r.getPeriod()))) {
                continue;
            }
            int months = asset.getInitDeprMonths() + own.size();
            BigDecimal accum = asset.getInitAccumDepr().add(sum(own));
            BigDecimal amount = FmsAssetCalculator.monthlyAmount(asset.getOriginalValue(), asset.getResidualValue(),
                    asset.getLifeMonths(), months, accum);
            if (amount.signum() <= 0) {
                continue;
            }
            Calc calc = new Calc();
            calc.asset = asset;
            calc.amount = amount;
            calc.accumAfter = accum.add(amount);
            calc.netAfter = asset.getOriginalValue().subtract(calc.accumAfter);
            calc.monthsAfter = months + 1;
            result.add(calc);
        }
        return result;
    }

    private void addRow(FmsAssetDepreciationRespVO resp, FmsAssetDepreciationRespVO.Row total,
            Map<Long, FmsAssetDepreciationRespVO.CategoryRow> categoryRows, FmsAssetDO asset,
            FmsAssetCategoryDO category, Map<Long, FmsSubjectDO> subjectMap, BigDecimal amount, BigDecimal accumAfter,
            BigDecimal netAfter, int months) {
        FmsAssetDepreciationRespVO.Row row = new FmsAssetDepreciationRespVO.Row();
        row.setAssetId(asset.getId());
        row.setCode(asset.getCode());
        row.setName(asset.getName());
        row.setCategoryName(category == null ? "" : category.getName());
        row.setOriginalValue(asset.getOriginalValue());
        row.setAmount(amount);
        row.setAccumAfter(accumAfter);
        row.setNetAfter(netAfter);
        row.setDepreciatedMonths(months);
        row.setLifeMonths(asset.getLifeMonths());
        resp.getRows().add(row);
        total.setOriginalValue(total.getOriginalValue().add(asset.getOriginalValue()));
        total.setAmount(total.getAmount().add(amount));
        total.setAccumAfter(total.getAccumAfter().add(accumAfter));
        total.setNetAfter(total.getNetAfter().add(netAfter));
        if (category != null) {
            FmsAssetDepreciationRespVO.CategoryRow categoryRow = categoryRows.computeIfAbsent(category.getId(), key -> {
                FmsAssetDepreciationRespVO.CategoryRow created = new FmsAssetDepreciationRespVO.CategoryRow();
                created.setCategoryName(category.getName());
                created.setExpenseSubjectName(subjectLabel(subjectMap.get(category.getExpenseSubjectId())));
                created.setAccumSubjectName(subjectLabel(subjectMap.get(category.getAccumSubjectId())));
                return created;
            });
            categoryRow.setAmount(categoryRow.getAmount().add(amount));
        }
    }

    private boolean addReconcileRow(FmsAssetReconcileRespVO resp, FmsSubjectDO subject, int type, BigDecimal card,
            BigDecimal ledger) {
        FmsAssetReconcileRespVO.Row row = new FmsAssetReconcileRespVO.Row();
        row.setSubjectName(subjectLabel(subject));
        row.setType(type);
        row.setCardAmount(card);
        row.setLedgerAmount(ledger);
        row.setDiff(card.subtract(ledger));
        resp.getRows().add(row);
        return row.getDiff().signum() == 0;
    }

    private void flattenBalance(List<FmsLedgerSubjectBalanceRespVO> nodes, Map<Long, FmsLedgerSubjectBalanceRespVO> map) {
        for (FmsLedgerSubjectBalanceRespVO node : CollUtil.emptyIfNull(nodes)) {
            // nodeType = 1 是科目本身；其它是辅助核算组合行，不能算进来
            if (ObjUtil.equal(node.getNodeType(), 1) && node.getSubjectId() != null) {
                map.put(node.getSubjectId(), node);
            }
            flattenBalance(node.getChildren(), map);
        }
    }

    private FmsAssetRespVO toResp(FmsAssetDO asset, FmsAssetCategoryDO category, List<FmsAssetDepreciationDO> records,
            YearMonth accountStart) {
        FmsAssetRespVO vo = new FmsAssetRespVO();
        vo.setId(asset.getId());
        vo.setCode(asset.getCode());
        vo.setName(asset.getName());
        vo.setCategoryId(asset.getCategoryId());
        vo.setCategoryName(category == null ? "" : category.getName());
        vo.setSpec(asset.getSpec());
        vo.setUnit(asset.getUnit());
        vo.setQuantity(asset.getQuantity());
        vo.setPurchaseDate(asset.getPurchaseDate());
        vo.setStartDate(asset.getStartDate());
        vo.setOriginalValue(asset.getOriginalValue());
        vo.setResidualRate(asset.getResidualRate());
        vo.setResidualValue(asset.getResidualValue());
        vo.setLifeMonths(asset.getLifeMonths());
        vo.setInitDeprMonths(asset.getInitDeprMonths());
        vo.setInitAccumDepr(asset.getInitAccumDepr());
        int months = asset.getInitDeprMonths() + records.size();
        BigDecimal accum = asset.getInitAccumDepr().add(sum(records));
        vo.setDepreciatedMonths(months);
        vo.setRemainMonths(Math.max(0, asset.getLifeMonths() - months));
        vo.setAccumDepr(accum);
        vo.setNetValue(asset.getOriginalValue().subtract(accum));
        vo.setMonthlyDepr(FmsAssetCalculator.standardMonthly(asset.getOriginalValue(), asset.getResidualValue(),
                asset.getLifeMonths()));
        YearMonth first = YearMonth.from(asset.getStartDate()).plusMonths(1);
        vo.setFirstDeprMonth((first.isBefore(accountStart) ? accountStart : first).toString());
        vo.setStatus(asset.getStatus());
        vo.setDisposeDate(asset.getDisposeDate());
        vo.setDisposeVoucherId(asset.getDisposeVoucherId());
        vo.setHasDepreciation(!records.isEmpty());
        vo.setRemark(asset.getRemark());
        return vo;
    }

    private static BigDecimal sum(List<FmsAssetDepreciationDO> records) {
        BigDecimal total = ZERO;
        for (FmsAssetDepreciationDO record : records) {
            total = total.add(record.getAmount());
        }
        return total;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static Map<Long, List<FmsAssetDepreciationDO>> groupByAsset(List<FmsAssetDepreciationDO> records) {
        Map<Long, List<FmsAssetDepreciationDO>> map = new HashMap<>();
        for (FmsAssetDepreciationDO record : records) {
            map.computeIfAbsent(record.getAssetId(), key -> new ArrayList<>()).add(record);
        }
        return map;
    }

    private static String latestPeriod(List<FmsAssetDepreciationDO> records) {
        String latest = null;
        for (FmsAssetDepreciationDO record : records) {
            if (latest == null || record.getPeriod().compareTo(latest) > 0) {
                latest = record.getPeriod();
            }
        }
        return latest;
    }

    private static YearMonth parsePeriod(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw exception(ASSET_PERIOD_INVALID);
        }
    }

    private static FmsVoucherEntrySaveReqVO entry(String digest, Long subjectId, BigDecimal debit, BigDecimal credit) {
        FmsVoucherEntrySaveReqVO entry = new FmsVoucherEntrySaveReqVO();
        entry.setDigest(digest);
        entry.setSubjectId(subjectId);
        entry.setDebitAmount(debit);
        entry.setCreditAmount(credit);
        entry.setAuxiliaries(new ArrayList<>());
        return entry;
    }

    private Long createVoucher(Long accountSetId, LocalDateTime voucherTime, List<FmsVoucherEntrySaveReqVO> entries,
            Long userId) {
        List<FmsVoucherWordDO> words = voucherWordService.getVoucherWordList(accountSetId);
        FmsVoucherWordDO word = CollUtil.findOne(words, w -> Boolean.TRUE.equals(w.getDefaultStatus()));
        Long wordId = word != null ? word.getId() : CollUtil.getFirst(words).getId();
        FmsVoucherSaveReqVO reqVO = new FmsVoucherSaveReqVO();
        reqVO.setAccountSetId(accountSetId);
        reqVO.setVoucherWordId(wordId);
        reqVO.setVoucherTime(voucherTime);
        reqVO.setVoucherNumber(voucherService.getNextVoucherNumber(accountSetId, wordId, voucherTime, userId));
        reqVO.setAttachmentCount(0);
        reqVO.setEntries(entries);
        return voucherService.createVoucher(reqVO, userId);
    }

    private void deleteVoucherIfExists(Long accountSetId, Long voucherId, Long userId) {
        if (voucherId == null) {
            return;
        }
        try {
            voucherService.deleteVoucherList(accountSetId, Collections.singletonList(voucherId), userId);
        } catch (ServiceException e) {
            // 凭证已被手工删除时忽略；其它原因（比如已审核）原样提示给用户
            if (!Objects.equals(e.getCode(), VOUCHER_NOT_EXISTS.getCode())) {
                throw e;
            }
        }
    }

    /**
     * 折旧凭证是否还有效：凭证被手工删除或作废了，对应的折旧明细就作废
     */
    private boolean isVoucherValid(Long accountSetId, Long voucherId, Long userId) {
        if (voucherId == null) {
            return false;
        }
        try {
            FmsVoucherDO voucher = voucherService.getVoucher(accountSetId, voucherId, userId);
            return voucher != null && !ObjUtil.equal(voucher.getStatus(), FmsVoucherStatusEnum.VOIDED.getStatus());
        } catch (ServiceException e) {
            if (Objects.equals(e.getCode(), VOUCHER_NOT_EXISTS.getCode())) {
                return false;
            }
            throw e;
        }
    }

    private String getVoucherNo(Long accountSetId, Long voucherId, Long userId) {
        try {
            FmsVoucherDO voucher = voucherService.getVoucher(accountSetId, voucherId, userId);
            if (voucher == null) {
                return null;
            }
            Map<Long, FmsVoucherWordDO> words = convertMap(voucherWordService.getVoucherWordList(accountSetId),
                    FmsVoucherWordDO::getId);
            FmsVoucherWordDO word = words.get(voucher.getVoucherWordId());
            return (word == null ? "" : word.getName() + "-") + voucher.getVoucherNumber();
        } catch (ServiceException e) {
            return null;
        }
    }

    /**
     * 加载折旧明细，凭证已经没了的明细算失效：heal 为 true 时顺便删掉
     */
    private List<FmsAssetDepreciationDO> loadRecords(Long accountSetId, Long userId, boolean heal) {
        List<FmsAssetDepreciationDO> valid = new ArrayList<>();
        List<Long> staleIds = new ArrayList<>();
        Map<Long, Boolean> checked = new HashMap<>();
        for (FmsAssetDepreciationDO record : depreciationMapper.selectListByAccountSetId(accountSetId)) {
            boolean ok = checked.computeIfAbsent(record.getVoucherId() == null ? -1L : record.getVoucherId(),
                    id -> isVoucherValid(accountSetId, id < 0 ? null : id, userId));
            if (ok) {
                valid.add(record);
            } else {
                staleIds.add(record.getId());
            }
        }
        if (heal && !staleIds.isEmpty()) {
            depreciationMapper.deleteByIds(staleIds);
        }
        return valid;
    }

    private List<FmsAssetDepreciationDO> getValidRecordsOfAsset(Long accountSetId, Long assetId, Long userId) {
        List<FmsAssetDepreciationDO> result = new ArrayList<>();
        for (FmsAssetDepreciationDO record : loadRecords(accountSetId, userId, false)) {
            if (ObjUtil.equal(record.getAssetId(), assetId)) {
                result.add(record);
            }
        }
        return result;
    }

    private Map<Long, FmsSubjectDO> getSubjectMap(Long accountSetId, Long userId) {
        return convertMap(subjectService.getSubjectList(accountSetId, null, userId), FmsSubjectDO::getId);
    }

    private FmsSubjectDO findSubjectByCode(Long accountSetId, String code, Long userId) {
        for (FmsSubjectDO subject : subjectService.getSubjectList(accountSetId, null, userId)) {
            if (code.equals(subject.getCode())) {
                return subject;
            }
        }
        throw exception(ASSET_CLEARING_SUBJECT_MISSING);
    }

    private void validateLeafSubject(Map<Long, FmsSubjectDO> subjectMap, Set<Long> parentIds, Long subjectId,
            String label) {
        FmsSubjectDO subject = subjectMap.get(subjectId);
        if (subject == null || parentIds.contains(subjectId)) {
            throw exception(ASSET_CATEGORY_SUBJECT_INVALID, label);
        }
    }

    private static String subjectLabel(FmsSubjectDO subject) {
        return subject == null ? "" : subject.getCode() + " " + subject.getName();
    }

    private FmsAssetCategoryDO validateCategory(Long accountSetId, Long id) {
        FmsAssetCategoryDO category = id == null ? null : categoryMapper.selectById(id);
        if (category == null || !ObjUtil.equal(category.getAccountSetId(), accountSetId)) {
            throw exception(ASSET_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private FmsAssetDO validateAsset(Long accountSetId, Long id) {
        FmsAssetDO asset = id == null ? null : assetMapper.selectById(id);
        if (asset == null || !ObjUtil.equal(asset.getAccountSetId(), accountSetId)) {
            throw exception(ASSET_NOT_EXISTS);
        }
        return asset;
    }

    /**
     * 自动生成资产编号：ZC0001、ZC0002 …
     */
    private String nextAssetCode(Long accountSetId) {
        int max = 0;
        for (FmsAssetDO asset : assetMapper.selectListByAccountSetId(accountSetId)) {
            String code = asset.getCode();
            if (code != null && code.startsWith("ZC") && code.substring(2).matches("\\d{1,8}")) {
                max = Math.max(max, Integer.parseInt(code.substring(2)));
            }
        }
        return String.format("ZC%04d", max + 1);
    }

}
