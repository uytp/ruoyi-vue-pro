package cn.iocoder.yudao.module.fms.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSettingVO;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSheetCalcReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSheetItemVO;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostSheetRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherEntrySaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryTypeDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsVoucherWordDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSettingDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSheetDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSheetItemDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherEntryDO;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsAuxiliaryItemMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsAuxiliaryTypeMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.cost.FmsCostSettingMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.cost.FmsCostSheetItemMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.cost.FmsCostSheetMapper;
import cn.iocoder.yudao.module.fms.enums.voucher.FmsVoucherStatusEnum;
import cn.iocoder.yudao.module.fms.service.closing.FmsClosingPeriodService;
import cn.iocoder.yudao.module.fms.service.config.FmsAccountSetService;
import cn.iocoder.yudao.module.fms.service.config.FmsSubjectService;
import cn.iocoder.yudao.module.fms.service.config.FmsVoucherWordService;
import cn.iocoder.yudao.module.fms.service.voucher.FmsVoucherService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.fms.service.cost.FmsCostCalculator.ZERO;
import static cn.iocoder.yudao.module.fms.service.cost.FmsCostCalculator.nvl;

/**
 * FMS 成本核算 Service 实现类（品种法，按月计算）
 */
@Service
@Validated
public class FmsCostServiceImpl implements FmsCostService {

    /** 辅助核算类别“存货”的类型值 */
    private static final int AUX_TYPE_INVENTORY = 6;

    @Resource
    private FmsCostSettingMapper settingMapper;
    @Resource
    private FmsCostSheetMapper sheetMapper;
    @Resource
    private FmsCostSheetItemMapper sheetItemMapper;
    @Resource
    private FmsAuxiliaryItemMapper auxiliaryItemMapper;
    @Resource
    private FmsAuxiliaryTypeMapper auxiliaryTypeMapper;

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

    // ==================== 成本参数 ====================

    @Override
    public FmsCostSettingVO getCostSetting(Long accountSetId, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        FmsCostSettingDO setting = settingMapper.selectByAccountSetId(accountSetId);
        if (setting != null) {
            return BeanUtils.toBean(setting, FmsCostSettingVO.class);
        }
        // 还没设置过：按常用科目编码和“存货”辅助核算给出默认值
        FmsCostSettingVO vo = new FmsCostSettingVO();
        vo.setAccountSetId(accountSetId);
        vo.setOverheadAllocMethod(FmsCostCalculator.ALLOC_BY_COMPLETED_QTY);
        vo.setWipMethod(0);
        vo.setMaterialInputMode(1);
        Map<String, Long> subjectIdByCode = new HashMap<>();
        for (FmsSubjectDO subject : subjectService.getSubjectList(accountSetId, null, userId)) {
            subjectIdByCode.putIfAbsent(subject.getCode(), subject.getId());
        }
        vo.setMaterialSubjectId(subjectIdByCode.get("400101"));
        vo.setLaborSubjectId(subjectIdByCode.get("400102"));
        vo.setOverheadInSubjectId(subjectIdByCode.get("400103"));
        vo.setOverheadPoolSubjectId(subjectIdByCode.get("4101"));
        vo.setFinishedSubjectId(subjectIdByCode.get("1405"));
        vo.setCogsSubjectId(subjectIdByCode.get("5401"));
        List<FmsAuxiliaryTypeDO> inventoryTypes = auxiliaryTypeMapper.selectList(
                new LambdaQueryWrapperX<FmsAuxiliaryTypeDO>()
                        .eq(FmsAuxiliaryTypeDO::getAccountSetId, accountSetId)
                        .eq(FmsAuxiliaryTypeDO::getType, AUX_TYPE_INVENTORY)
                        .orderByAsc(FmsAuxiliaryTypeDO::getId));
        if (CollUtil.isNotEmpty(inventoryTypes)) {
            vo.setProductAuxTypeId(inventoryTypes.get(0).getId());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCostSetting(FmsCostSettingVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        validateSetting(reqVO, userId);
        FmsCostSettingDO setting = BeanUtils.toBean(reqVO, FmsCostSettingDO.class);
        FmsCostSettingDO existing = settingMapper.selectByAccountSetId(accountSetId);
        if (existing == null) {
            settingMapper.insert(setting);
        } else {
            setting.setId(existing.getId());
            settingMapper.updateById(setting);
        }
    }

    /**
     * 校验成本参数里的科目：必须存在、启用、是末级科目；归集产品成本的科目必须启用“产品”辅助核算
     */
    private void validateSetting(FmsCostSettingVO vo, Long userId) {
        Long accountSetId = vo.getAccountSetId();
        FmsAuxiliaryTypeDO auxType = auxiliaryTypeMapper.selectById(vo.getProductAuxTypeId());
        if (auxType == null || !Objects.equals(auxType.getAccountSetId(), accountSetId)) {
            throw exception(COST_PRODUCT_AUX_TYPE_INVALID);
        }
        List<FmsSubjectDO> subjects = subjectService.getSubjectList(accountSetId, null, userId);
        Map<Long, FmsSubjectDO> subjectMap = new HashMap<>();
        Set<Long> parentIds = new HashSet<>();
        for (FmsSubjectDO subject : subjects) {
            subjectMap.put(subject.getId(), subject);
            parentIds.add(subject.getParentId());
        }
        Object[][] checks = {
                {"生产成本-直接材料", vo.getMaterialSubjectId(), true},
                {"生产成本-直接人工", vo.getLaborSubjectId(), true},
                {"生产成本-制造费用", vo.getOverheadInSubjectId(), true},
                {"制造费用", vo.getOverheadPoolSubjectId(), false},
                {"库存商品", vo.getFinishedSubjectId(), false},
                {"主营业务成本", vo.getCogsSubjectId(), false}};
        for (Object[] check : checks) {
            String label = (String) check[0];
            FmsSubjectDO subject = subjectMap.get((Long) check[1]);
            if (subject == null || parentIds.contains(subject.getId())
                    || !Objects.equals(subject.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
                throw exception(COST_SUBJECT_INVALID, label);
            }
            if ((Boolean) check[2] && (subject.getAuxiliaryTypeIds() == null
                    || !subject.getAuxiliaryTypeIds().contains(vo.getProductAuxTypeId()))) {
                throw exception(COST_SUBJECT_AUX_REQUIRED, subject.getName(), auxType.getName());
            }
        }
    }

    private FmsCostSettingDO requireSetting(Long accountSetId) {
        FmsCostSettingDO setting = settingMapper.selectByAccountSetId(accountSetId);
        if (setting == null) {
            throw exception(COST_SETTING_NOT_CONFIGURED);
        }
        return setting;
    }

    // ==================== 成本计算单 ====================

    @Override
    public FmsCostSheetRespVO getCostSheet(Long accountSetId, String period, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        String month = parseMonth(period).toString();
        FmsCostSheetDO sheet = sheetMapper.selectByAccountSetIdAndPeriod(accountSetId, month);
        if (sheet == null) {
            FmsCostSheetRespVO empty = new FmsCostSheetRespVO();
            empty.setAccountSetId(accountSetId);
            empty.setPeriod(month);
            empty.setStatus(FmsCostSheetDO.STATUS_DRAFT);
            empty.setOverheadTotal(ZERO);
            return empty;
        }
        return buildResp(sheet, sheetItemMapper.selectListBySheetId(sheet.getId()), Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FmsCostSheetRespVO calculateCostSheet(FmsCostSheetCalcReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        YearMonth month = parseMonth(reqVO.getPeriod());
        String period = month.toString();
        validatePeriodOpen(accountSetId, period);
        FmsCostSettingDO setting = requireSetting(accountSetId);
        FmsCostSheetDO sheet = sheetMapper.selectByAccountSetIdAndPeriod(accountSetId, period);
        if (sheet != null && Objects.equals(sheet.getStatus(), FmsCostSheetDO.STATUS_GENERATED)) {
            throw exception(COST_SHEET_VOUCHER_GENERATED);
        }
        List<String> warnings = new ArrayList<>();

        // 1. 手工录入：先沿用上次保存的，再用本次请求覆盖
        Map<Long, FmsCostSheetItemDO> rows = new LinkedHashMap<>();
        if (sheet != null) {
            for (FmsCostSheetItemDO old : sheetItemMapper.selectListBySheetId(sheet.getId())) {
                FmsCostSheetItemDO row = newRow(accountSetId, old.getProductItemId());
                copyInputs(old, row);
                rows.put(old.getProductItemId(), row);
            }
        }
        if (CollUtil.isNotEmpty(reqVO.getItems())) {
            for (FmsCostSheetItemVO input : reqVO.getItems()) {
                FmsCostSheetItemDO row = rows.computeIfAbsent(input.getProductItemId(),
                        id -> newRow(accountSetId, id));
                applyInput(input, row);
            }
        }

        // 2. 期初：上月有计算单时，期初在产品和期初库存自动取上月期末
        FmsCostSheetDO prevSheet = sheetMapper.selectByAccountSetIdAndPeriod(accountSetId, month.minusMonths(1).toString());
        if (prevSheet != null) {
            for (FmsCostSheetItemDO prev : sheetItemMapper.selectListBySheetId(prevSheet.getId())) {
                boolean hasBalance = nvl(prev.getEndWipMaterial()).signum() != 0
                        || nvl(prev.getEndWipLabor()).signum() != 0
                        || nvl(prev.getEndWipOverhead()).signum() != 0
                        || nvl(prev.getEndStockQty()).signum() != 0
                        || nvl(prev.getEndStockAmount()).signum() != 0;
                if (!hasBalance && !rows.containsKey(prev.getProductItemId())) {
                    continue;
                }
                FmsCostSheetItemDO row = rows.computeIfAbsent(prev.getProductItemId(),
                        id -> newRow(accountSetId, id));
                row.setBeginWipMaterial(nvl(prev.getEndWipMaterial()));
                row.setBeginWipLabor(nvl(prev.getEndWipLabor()));
                row.setBeginWipOverhead(nvl(prev.getEndWipOverhead()));
                row.setBeginStockQty(nvl(prev.getEndStockQty()));
                row.setBeginStockAmount(nvl(prev.getEndStockAmount()));
            }
        }

        // 3. 归集：从本月凭证里取直接材料、直接人工（按产品）和制造费用（整体）
        BigDecimal overheadPool = collectCosts(accountSetId, month, setting, rows, warnings);

        // 4. 补全产品名称
        fillProductInfo(accountSetId, rows.values());

        // 5. 分配制造费用并逐个产品计算
        List<FmsCostSheetItemDO> items = new ArrayList<>(rows.values());
        items.sort(Comparator.comparing((FmsCostSheetItemDO i) -> i.getProductCode() == null ? "" : i.getProductCode())
                .thenComparing(FmsCostSheetItemDO::getProductItemId));
        int allocMethod = setting.getOverheadAllocMethod();
        if (!FmsCostCalculator.allocateOverhead(items, overheadPool, allocMethod)) {
            throw exception(COST_ALLOC_BASE_EMPTY, overheadPool.toPlainString(),
                    FmsCostCalculator.allocBaseName(allocMethod));
        }
        for (FmsCostSheetItemDO item : items) {
            BigDecimal available = FmsCostCalculator.computeItem(item, setting.getWipMethod(),
                    setting.getMaterialInputMode());
            if (available != null) {
                throw exception(COST_SALES_QTY_EXCEED, item.getProductName(),
                        item.getSalesQty().stripTrailingZeros().toPlainString(),
                        available.stripTrailingZeros().toPlainString());
            }
        }

        // 6. 保存
        if (sheet == null) {
            sheet = new FmsCostSheetDO();
            sheet.setAccountSetId(accountSetId);
            sheet.setPeriod(period);
            sheet.setStatus(FmsCostSheetDO.STATUS_DRAFT);
            sheet.setOverheadTotal(overheadPool);
            sheetMapper.insert(sheet);
        } else {
            sheet.setStatus(FmsCostSheetDO.STATUS_DRAFT);
            sheet.setOverheadTotal(overheadPool);
            sheetMapper.updateById(sheet);
            sheetItemMapper.deleteBySheetId(sheet.getId());
        }
        for (FmsCostSheetItemDO item : items) {
            item.setSheetId(sheet.getId());
        }
        if (!items.isEmpty()) {
            sheetItemMapper.insertBatch(items);
        }
        return buildResp(sheet, items, warnings);
    }

    /**
     * 从本月凭证归集成本，结果写入 rows 的 curMaterial / curLabor，并返回本期制造费用归集总额
     */
    private BigDecimal collectCosts(Long accountSetId, YearMonth month, FmsCostSettingDO setting,
            Map<Long, FmsCostSheetItemDO> rows, List<String> warnings) {
        LocalDateTime beginTime = LocalDateTimeUtils.getMonthBeginTime(month);
        LocalDateTime endTime = LocalDateTimeUtils.endOfMonth(beginTime);
        Set<Long> voucherIds = new HashSet<>();
        for (FmsVoucherDO voucher : voucherService.getVoucherListByPeriod(accountSetId, beginTime, endTime)) {
            if (!Objects.equals(voucher.getStatus(), FmsVoucherStatusEnum.VOIDED.getStatus())) {
                voucherIds.add(voucher.getId());
            }
        }
        BigDecimal overheadPool = ZERO;
        BigDecimal unassignedMaterial = ZERO;
        BigDecimal unassignedLabor = ZERO;
        for (FmsVoucherEntryDO entry : voucherService.getVoucherEntryList(voucherIds)) {
            BigDecimal net = nvl(entry.getDebitAmount()).subtract(nvl(entry.getCreditAmount()));
            if (net.signum() == 0) {
                continue;
            }
            Long subjectId = entry.getSubjectId();
            if (Objects.equals(subjectId, setting.getOverheadPoolSubjectId())) {
                overheadPool = overheadPool.add(net);
                continue;
            }
            boolean isMaterial = Objects.equals(subjectId, setting.getMaterialSubjectId());
            boolean isLabor = Objects.equals(subjectId, setting.getLaborSubjectId());
            if (!isMaterial && !isLabor) {
                continue;
            }
            Long productItemId = findProductItemId(entry, setting.getProductAuxTypeId());
            if (productItemId == null) {
                if (isMaterial) {
                    unassignedMaterial = unassignedMaterial.add(net);
                } else {
                    unassignedLabor = unassignedLabor.add(net);
                }
                continue;
            }
            FmsCostSheetItemDO row = rows.computeIfAbsent(productItemId, id -> newRow(accountSetId, id));
            if (isMaterial) {
                row.setCurMaterial(row.getCurMaterial().add(net));
            } else {
                row.setCurLabor(row.getCurLabor().add(net));
            }
        }
        if (unassignedMaterial.signum() != 0) {
            warnings.add("有【直接材料】分录没有选择产品，金额 " + unassignedMaterial.toPlainString() + " 元没有被归集");
        }
        if (unassignedLabor.signum() != 0) {
            warnings.add("有【直接人工】分录没有选择产品，金额 " + unassignedLabor.toPlainString() + " 元没有被归集");
        }
        return overheadPool;
    }

    private static Long findProductItemId(FmsVoucherEntryDO entry, Long productAuxTypeId) {
        if (CollUtil.isEmpty(entry.getAuxiliaries())) {
            return null;
        }
        for (FmsVoucherEntryDO.AuxiliaryItem aux : entry.getAuxiliaries()) {
            if (Objects.equals(aux.getTypeId(), productAuxTypeId)) {
                return aux.getItemId();
            }
        }
        return null;
    }

    private void fillProductInfo(Long accountSetId, Collection<FmsCostSheetItemDO> rows) {
        if (rows.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        rows.forEach(row -> ids.add(row.getProductItemId()));
        Map<Long, FmsAuxiliaryItemDO> itemMap = new HashMap<>();
        for (FmsAuxiliaryItemDO item : auxiliaryItemMapper.selectListByIdsAndAccountSetId(ids, accountSetId)) {
            itemMap.put(item.getId(), item);
        }
        for (FmsCostSheetItemDO row : rows) {
            FmsAuxiliaryItemDO item = itemMap.get(row.getProductItemId());
            if (item == null) {
                row.setProductCode("");
                row.setProductName("（产品已删除）" + row.getProductItemId());
                row.setUnit("");
            } else {
                row.setProductCode(item.getCode());
                row.setProductName(item.getName());
                row.setUnit(item.getUnit());
            }
        }
    }

    private static FmsCostSheetItemDO newRow(Long accountSetId, Long productItemId) {
        FmsCostSheetItemDO row = new FmsCostSheetItemDO();
        row.setAccountSetId(accountSetId);
        row.setProductItemId(productItemId);
        row.setWorkHours(ZERO);
        row.setCompletedQty(ZERO);
        row.setWipQty(ZERO);
        row.setWipProgress(new BigDecimal("100"));
        row.setSalesQty(ZERO);
        row.setBeginWipMaterial(ZERO);
        row.setBeginWipLabor(ZERO);
        row.setBeginWipOverhead(ZERO);
        row.setBeginStockQty(ZERO);
        row.setBeginStockAmount(ZERO);
        row.setCurMaterial(ZERO);
        row.setCurLabor(ZERO);
        row.setAllocOverhead(ZERO);
        row.setEndWipMaterial(ZERO);
        row.setEndWipLabor(ZERO);
        row.setEndWipOverhead(ZERO);
        row.setDoneMaterial(ZERO);
        row.setDoneLabor(ZERO);
        row.setDoneOverhead(ZERO);
        row.setDoneTotal(ZERO);
        row.setUnitCost(ZERO);
        row.setCogsAmount(ZERO);
        row.setEndStockQty(ZERO);
        row.setEndStockAmount(ZERO);
        return row;
    }

    private static void copyInputs(FmsCostSheetItemDO from, FmsCostSheetItemDO to) {
        to.setWorkHours(nvl(from.getWorkHours()));
        to.setCompletedQty(nvl(from.getCompletedQty()));
        to.setWipQty(nvl(from.getWipQty()));
        to.setWipProgress(from.getWipProgress() == null ? new BigDecimal("100") : from.getWipProgress());
        to.setSalesQty(nvl(from.getSalesQty()));
        to.setBeginWipMaterial(nvl(from.getBeginWipMaterial()));
        to.setBeginWipLabor(nvl(from.getBeginWipLabor()));
        to.setBeginWipOverhead(nvl(from.getBeginWipOverhead()));
        to.setBeginStockQty(nvl(from.getBeginStockQty()));
        to.setBeginStockAmount(nvl(from.getBeginStockAmount()));
    }

    private static void applyInput(FmsCostSheetItemVO in, FmsCostSheetItemDO row) {
        if (in.getWorkHours() != null) {
            row.setWorkHours(in.getWorkHours());
        }
        if (in.getCompletedQty() != null) {
            row.setCompletedQty(in.getCompletedQty());
        }
        if (in.getWipQty() != null) {
            row.setWipQty(in.getWipQty());
        }
        if (in.getWipProgress() != null) {
            row.setWipProgress(in.getWipProgress());
        }
        if (in.getSalesQty() != null) {
            row.setSalesQty(in.getSalesQty());
        }
        if (in.getBeginWipMaterial() != null) {
            row.setBeginWipMaterial(in.getBeginWipMaterial());
        }
        if (in.getBeginWipLabor() != null) {
            row.setBeginWipLabor(in.getBeginWipLabor());
        }
        if (in.getBeginWipOverhead() != null) {
            row.setBeginWipOverhead(in.getBeginWipOverhead());
        }
        if (in.getBeginStockQty() != null) {
            row.setBeginStockQty(in.getBeginStockQty());
        }
        if (in.getBeginStockAmount() != null) {
            row.setBeginStockAmount(in.getBeginStockAmount());
        }
    }

    private FmsCostSheetRespVO buildResp(FmsCostSheetDO sheet, List<FmsCostSheetItemDO> items, List<String> warnings) {
        FmsCostSheetRespVO resp = new FmsCostSheetRespVO();
        resp.setId(sheet.getId());
        resp.setAccountSetId(sheet.getAccountSetId());
        resp.setPeriod(sheet.getPeriod());
        resp.setStatus(sheet.getStatus());
        resp.setOverheadTotal(nvl(sheet.getOverheadTotal()));
        resp.setAllocVoucherId(sheet.getAllocVoucherId());
        resp.setDoneVoucherId(sheet.getDoneVoucherId());
        resp.setCogsVoucherId(sheet.getCogsVoucherId());
        resp.setItems(BeanUtils.toBean(items, FmsCostSheetItemVO.class));
        resp.setWarnings(new ArrayList<>(warnings));
        return resp;
    }

    private YearMonth parseMonth(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw exception(COST_PERIOD_INVALID);
        }
    }

    private void validatePeriodOpen(Long accountSetId, String period) {
        if (closingPeriodService.isPeriodClosed(accountSetId, period)) {
            throw exception(COST_PERIOD_CLOSED);
        }
    }

    // ==================== 生成 / 撤销凭证 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FmsCostSheetRespVO generateCostVouchers(Long accountSetId, String period, Long userId) {
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        YearMonth month = parseMonth(period);
        String monthText = month.toString();
        validatePeriodOpen(accountSetId, monthText);
        FmsCostSettingDO setting = requireSetting(accountSetId);
        FmsCostSheetDO sheet = sheetMapper.selectByAccountSetIdAndPeriod(accountSetId, monthText);
        if (sheet == null) {
            throw exception(COST_SHEET_NOT_EXISTS);
        }
        if (Objects.equals(sheet.getStatus(), FmsCostSheetDO.STATUS_GENERATED)) {
            throw exception(COST_SHEET_VOUCHER_GENERATED);
        }
        List<FmsCostSheetItemDO> items = sheetItemMapper.selectListBySheetId(sheet.getId());
        Map<Long, FmsSubjectDO> subjectMap = new HashMap<>();
        for (FmsSubjectDO subject : subjectService.getSubjectList(accountSetId, null, userId)) {
            subjectMap.put(subject.getId(), subject);
        }
        LocalDateTime voucherTime = month.atEndOfMonth().atStartOfDay();
        Long voucherWordId = getDefaultVoucherWordId(accountSetId);
        Long auxTypeId = setting.getProductAuxTypeId();
        FmsSubjectDO materialSubject = subjectMap.get(setting.getMaterialSubjectId());
        FmsSubjectDO laborSubject = subjectMap.get(setting.getLaborSubjectId());
        FmsSubjectDO overheadInSubject = subjectMap.get(setting.getOverheadInSubjectId());
        FmsSubjectDO poolSubject = subjectMap.get(setting.getOverheadPoolSubjectId());
        FmsSubjectDO finishedSubject = subjectMap.get(setting.getFinishedSubjectId());
        FmsSubjectDO cogsSubject = subjectMap.get(setting.getCogsSubjectId());
        for (FmsSubjectDO subject : Arrays.asList(materialSubject, laborSubject, overheadInSubject,
                poolSubject, finishedSubject, cogsSubject)) {
            if (subject == null) {
                throw exception(COST_SUBJECT_INVALID, "成本参数");
            }
        }

        // 1. 制造费用分配：借 生产成本-制造费用（按产品），贷 制造费用
        List<FmsVoucherEntrySaveReqVO> allocEntries = new ArrayList<>();
        BigDecimal allocTotal = ZERO;
        for (FmsCostSheetItemDO item : items) {
            BigDecimal amount = nvl(item.getAllocOverhead());
            if (amount.signum() != 0) {
                allocEntries.add(entry("分配" + monthText + "制造费用-" + item.getProductName(),
                        overheadInSubject, amount, auxTypeId, item.getProductItemId()));
                allocTotal = allocTotal.add(amount);
            }
        }
        if (allocTotal.signum() != 0) {
            allocEntries.add(entry("分配" + monthText + "制造费用", poolSubject, allocTotal.negate(), auxTypeId, null));
        }

        // 2. 完工入库：借 库存商品（按产品），贷 生产成本-直接材料 / 直接人工 / 制造费用（按产品）
        List<FmsVoucherEntrySaveReqVO> doneEntries = new ArrayList<>();
        for (FmsCostSheetItemDO item : items) {
            BigDecimal doneTotal = nvl(item.getDoneTotal());
            if (doneTotal.signum() == 0) {
                continue;
            }
            String name = item.getProductName();
            doneEntries.add(entry("产品完工入库-" + name, finishedSubject, doneTotal, auxTypeId, item.getProductItemId()));
            addCredit(doneEntries, "结转完工产品直接材料-" + name, materialSubject, item.getDoneMaterial(), auxTypeId, item.getProductItemId());
            addCredit(doneEntries, "结转完工产品直接人工-" + name, laborSubject, item.getDoneLabor(), auxTypeId, item.getProductItemId());
            addCredit(doneEntries, "结转完工产品制造费用-" + name, overheadInSubject, item.getDoneOverhead(), auxTypeId, item.getProductItemId());
        }

        // 3. 销售成本结转：借 主营业务成本，贷 库存商品（均按产品）
        List<FmsVoucherEntrySaveReqVO> cogsEntries = new ArrayList<>();
        for (FmsCostSheetItemDO item : items) {
            BigDecimal cogs = nvl(item.getCogsAmount());
            if (cogs.signum() == 0) {
                continue;
            }
            cogsEntries.add(entry("结转销售成本-" + item.getProductName(), cogsSubject, cogs, auxTypeId, item.getProductItemId()));
            cogsEntries.add(entry("结转销售成本-" + item.getProductName(), finishedSubject, cogs.negate(), auxTypeId, item.getProductItemId()));
        }
        if (allocEntries.isEmpty() && doneEntries.isEmpty() && cogsEntries.isEmpty()) {
            throw exception(COST_NOTHING_TO_GENERATE);
        }

        sheet.setAllocVoucherId(createVoucher(accountSetId, voucherWordId, voucherTime, allocEntries, userId));
        sheet.setDoneVoucherId(createVoucher(accountSetId, voucherWordId, voucherTime, doneEntries, userId));
        sheet.setCogsVoucherId(createVoucher(accountSetId, voucherWordId, voucherTime, cogsEntries, userId));
        sheet.setStatus(FmsCostSheetDO.STATUS_GENERATED);
        sheetMapper.updateById(sheet);
        return buildResp(sheet, items, Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FmsCostSheetRespVO revokeCostVouchers(Long accountSetId, String period, Long userId) {
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        String monthText = parseMonth(period).toString();
        validatePeriodOpen(accountSetId, monthText);
        FmsCostSheetDO sheet = sheetMapper.selectByAccountSetIdAndPeriod(accountSetId, monthText);
        if (sheet == null) {
            throw exception(COST_SHEET_NOT_EXISTS);
        }
        if (!Objects.equals(sheet.getStatus(), FmsCostSheetDO.STATUS_GENERATED)) {
            throw exception(COST_VOUCHER_NOT_GENERATED);
        }
        for (Long voucherId : Arrays.asList(sheet.getAllocVoucherId(), sheet.getDoneVoucherId(), sheet.getCogsVoucherId())) {
            deleteVoucherIfExists(accountSetId, voucherId, userId);
        }
        sheet.setAllocVoucherId(null);
        sheet.setDoneVoucherId(null);
        sheet.setCogsVoucherId(null);
        sheet.setStatus(FmsCostSheetDO.STATUS_DRAFT);
        // updateById 默认忽略 null，这里需要把凭证编号清空
        sheetMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FmsCostSheetDO>()
                .eq(FmsCostSheetDO::getId, sheet.getId())
                .set(FmsCostSheetDO::getAllocVoucherId, null)
                .set(FmsCostSheetDO::getDoneVoucherId, null)
                .set(FmsCostSheetDO::getCogsVoucherId, null)
                .set(FmsCostSheetDO::getStatus, FmsCostSheetDO.STATUS_DRAFT));
        return buildResp(sheet, sheetItemMapper.selectListBySheetId(sheet.getId()), Collections.emptyList());
    }

    private void deleteVoucherIfExists(Long accountSetId, Long voucherId, Long userId) {
        if (voucherId == null) {
            return;
        }
        try {
            voucherService.deleteVoucherList(accountSetId, Collections.singletonList(voucherId), userId);
        } catch (ServiceException e) {
            // 凭证已被手工删除时忽略；其它原因（如已审核）原样提示给用户
            if (!Objects.equals(e.getCode(), VOUCHER_NOT_EXISTS.getCode())) {
                throw e;
            }
        }
    }

    private Long getDefaultVoucherWordId(Long accountSetId) {
        List<FmsVoucherWordDO> words = voucherWordService.getVoucherWordList(accountSetId);
        FmsVoucherWordDO word = CollUtil.findOne(words, w -> Boolean.TRUE.equals(w.getDefaultStatus()));
        return word != null ? word.getId() : CollUtil.getFirst(words).getId();
    }

    private Long createVoucher(Long accountSetId, Long voucherWordId, LocalDateTime voucherTime,
            List<FmsVoucherEntrySaveReqVO> entries, Long userId) {
        if (entries.isEmpty()) {
            return null;
        }
        FmsVoucherSaveReqVO reqVO = new FmsVoucherSaveReqVO();
        reqVO.setAccountSetId(accountSetId);
        reqVO.setVoucherWordId(voucherWordId);
        reqVO.setVoucherTime(voucherTime);
        reqVO.setVoucherNumber(voucherService.getNextVoucherNumber(accountSetId, voucherWordId, voucherTime, userId));
        reqVO.setAttachmentCount(0);
        reqVO.setEntries(entries);
        return voucherService.createVoucher(reqVO, userId);
    }

    /**
     * 生成一条分录；signedDebit 为正记借方，为负记贷方。科目启用了产品辅助核算时带上产品
     */
    private static FmsVoucherEntrySaveReqVO entry(String digest, FmsSubjectDO subject, BigDecimal signedDebit,
            Long productAuxTypeId, Long productItemId) {
        FmsVoucherEntrySaveReqVO entry = new FmsVoucherEntrySaveReqVO();
        entry.setDigest(digest);
        entry.setSubjectId(subject.getId());
        entry.setDebitAmount(signedDebit.signum() > 0 ? signedDebit : ZERO);
        entry.setCreditAmount(signedDebit.signum() < 0 ? signedDebit.negate() : ZERO);
        List<FmsVoucherEntrySaveReqVO.AuxiliaryItem> auxiliaries = new ArrayList<>();
        if (productItemId != null && subject.getAuxiliaryTypeIds() != null
                && subject.getAuxiliaryTypeIds().contains(productAuxTypeId)) {
            FmsVoucherEntrySaveReqVO.AuxiliaryItem aux = new FmsVoucherEntrySaveReqVO.AuxiliaryItem();
            aux.setTypeId(productAuxTypeId);
            aux.setItemId(productItemId);
            auxiliaries.add(aux);
        }
        entry.setAuxiliaries(auxiliaries);
        return entry;
    }

    private static void addCredit(List<FmsVoucherEntrySaveReqVO> entries, String digest, FmsSubjectDO subject,
            BigDecimal amount, Long productAuxTypeId, Long productItemId) {
        if (nvl(amount).signum() != 0) {
            entries.add(entry(digest, subject, amount.negate(), productAuxTypeId, productItemId));
        }
    }

}
