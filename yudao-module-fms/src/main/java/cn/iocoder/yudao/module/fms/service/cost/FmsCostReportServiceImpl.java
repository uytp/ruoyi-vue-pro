package cn.iocoder.yudao.module.fms.service.cost;

import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostReportRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.cost.vo.FmsCostReportRowVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSheetDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.cost.FmsCostSheetItemDO;
import cn.iocoder.yudao.module.fms.dal.mysql.cost.FmsCostSheetItemMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.cost.FmsCostSheetMapper;
import cn.iocoder.yudao.module.fms.service.config.FmsAccountSetService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.fms.service.cost.FmsCostCalculator.ZERO;
import static cn.iocoder.yudao.module.fms.service.cost.FmsCostCalculator.nvl;

/**
 * FMS 成本报表 Service 实现类
 */
@Service
@Validated
public class FmsCostReportServiceImpl implements FmsCostReportService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    /** 一次最多查询的月数 */
    private static final int MAX_MONTHS = 36;

    @Resource
    private FmsCostSheetMapper sheetMapper;
    @Resource
    private FmsCostSheetItemMapper sheetItemMapper;
    @Resource
    private FmsAccountSetService accountSetService;

    @Override
    public FmsCostReportRespVO getCostReport(Long accountSetId, String startPeriod, String endPeriod,
            boolean includeDraft, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        YearMonth start = parseMonth(startPeriod);
        YearMonth end = parseMonth(endPeriod);
        if (start.isAfter(end)) {
            throw exception(COST_REPORT_RANGE_INVALID);
        }
        if (ChronoUnit.MONTHS.between(start, end) >= MAX_MONTHS) {
            throw exception(COST_REPORT_RANGE_TOO_LONG);
        }

        FmsCostReportRespVO resp = new FmsCostReportRespVO();
        resp.setStartPeriod(start.toString());
        resp.setEndPeriod(end.toString());

        // 1. 取范围内的成本单和明细
        List<FmsCostSheetDO> sheets = sheetMapper.selectListByPeriodRange(accountSetId, start.toString(),
                end.toString(), !includeDraft);
        resp.setSheetCount(sheets.size());
        resp.setDraftCount((int) sheets.stream()
                .filter(s -> !Objects.equals(s.getStatus(), FmsCostSheetDO.STATUS_GENERATED)).count());
        if (sheets.isEmpty()) {
            resp.setTotal(emptyRow("合计"));
            return resp;
        }
        Map<Long, FmsCostSheetDO> sheetMap = new HashMap<>();
        sheets.forEach(s -> sheetMap.put(s.getId(), s));
        List<FmsCostSheetItemDO> items = sheetItemMapper.selectListBySheetIds(sheetMap.keySet());
        // 按 产品编码、月份 排序，明细表和汇总表都按这个顺序出
        items.sort(Comparator
                .comparing((FmsCostSheetItemDO i) -> nullToEmpty(i.getProductCode()))
                .thenComparing(i -> nullToEmpty(i.getProductName()))
                .thenComparing(i -> sheetMap.get(i.getSheetId()).getPeriod()));

        // 2. 明细表 + 汇总表
        Map<Long, FmsCostReportRowVO> summaryMap = new LinkedHashMap<>();
        Map<Long, String> lastPeriodMap = new HashMap<>();
        for (FmsCostSheetItemDO item : items) {
            FmsCostSheetDO sheet = sheetMap.get(item.getSheetId());
            FmsCostReportRowVO detail = toRow(item);
            detail.setPeriod(sheet.getPeriod());
            detail.setStatus(sheet.getStatus());
            fillRates(detail);
            resp.getDetail().add(detail);

            FmsCostReportRowVO sum = summaryMap.computeIfAbsent(item.getProductItemId(), id -> {
                FmsCostReportRowVO row = emptyRow(null);
                row.setProductItemId(id);
                row.setProductCode(item.getProductCode());
                row.setProductName(item.getProductName());
                row.setUnit(item.getUnit());
                return row;
            });
            addFlow(sum, detail);
            // 期末余额类取范围内最后一个月（明细已按月份升序，后面的覆盖前面的）
            String last = lastPeriodMap.get(item.getProductItemId());
            if (last == null || sheet.getPeriod().compareTo(last) >= 0) {
                lastPeriodMap.put(item.getProductItemId(), sheet.getPeriod());
                sum.setEndWipTotal(detail.getEndWipTotal());
                sum.setEndStockQty(detail.getEndStockQty());
                sum.setEndStockAmount(detail.getEndStockAmount());
            }
        }
        FmsCostReportRowVO total = emptyRow("合计");
        for (FmsCostReportRowVO sum : summaryMap.values()) {
            fillRates(sum);
            resp.getSummary().add(sum);
            addFlow(total, sum);
            total.setEndWipTotal(total.getEndWipTotal().add(sum.getEndWipTotal()));
            total.setEndStockQty(total.getEndStockQty().add(sum.getEndStockQty()));
            total.setEndStockAmount(total.getEndStockAmount().add(sum.getEndStockAmount()));
        }
        // 合计行的单位成本没有意义（不同产品混在一起），占比有意义
        fillRates(total);
        total.setUnitCost(null);
        total.setCompletedQty(null);
        total.setSalesQty(null);
        total.setEndStockQty(null);
        resp.setTotal(total);
        return resp;
    }

    /** 明细行：把成本单明细的字段搬过来，期末在产品合并成一个金额 */
    private FmsCostReportRowVO toRow(FmsCostSheetItemDO item) {
        FmsCostReportRowVO row = new FmsCostReportRowVO();
        row.setProductItemId(item.getProductItemId());
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setUnit(item.getUnit());
        row.setCompletedQty(nvl(item.getCompletedQty()));
        row.setDoneMaterial(nvl(item.getDoneMaterial()));
        row.setDoneLabor(nvl(item.getDoneLabor()));
        row.setDoneOverhead(nvl(item.getDoneOverhead()));
        row.setDoneTotal(nvl(item.getDoneTotal()));
        row.setUnitCost(nvl(item.getUnitCost()));
        row.setSalesQty(nvl(item.getSalesQty()));
        row.setCogsAmount(nvl(item.getCogsAmount()));
        row.setEndWipTotal(nvl(item.getEndWipMaterial()).add(nvl(item.getEndWipLabor()))
                .add(nvl(item.getEndWipOverhead())));
        row.setEndStockQty(nvl(item.getEndStockQty()));
        row.setEndStockAmount(nvl(item.getEndStockAmount()));
        return row;
    }

    /** 全是 0 的行 */
    private FmsCostReportRowVO emptyRow(String productName) {
        FmsCostReportRowVO row = new FmsCostReportRowVO();
        row.setProductName(productName);
        row.setCompletedQty(ZERO);
        row.setDoneMaterial(ZERO);
        row.setDoneLabor(ZERO);
        row.setDoneOverhead(ZERO);
        row.setDoneTotal(ZERO);
        row.setUnitCost(ZERO);
        row.setMaterialRate(ZERO);
        row.setLaborRate(ZERO);
        row.setOverheadRate(ZERO);
        row.setSalesQty(ZERO);
        row.setCogsAmount(ZERO);
        row.setEndWipTotal(ZERO);
        row.setEndStockQty(ZERO);
        row.setEndStockAmount(ZERO);
        return row;
    }

    /** 把 from 行的“流量类”数据（完工、销售）累加到 to 行 */
    private void addFlow(FmsCostReportRowVO to, FmsCostReportRowVO from) {
        to.setCompletedQty(to.getCompletedQty().add(from.getCompletedQty()));
        to.setDoneMaterial(to.getDoneMaterial().add(from.getDoneMaterial()));
        to.setDoneLabor(to.getDoneLabor().add(from.getDoneLabor()));
        to.setDoneOverhead(to.getDoneOverhead().add(from.getDoneOverhead()));
        to.setDoneTotal(to.getDoneTotal().add(from.getDoneTotal()));
        to.setSalesQty(to.getSalesQty().add(from.getSalesQty()));
        to.setCogsAmount(to.getCogsAmount().add(from.getCogsAmount()));
    }

    /** 补算单位成本和成本构成占比（汇总行累加后需要重算，明细行也统一走这里） */
    private void fillRates(FmsCostReportRowVO row) {
        BigDecimal total = row.getDoneTotal();
        if (row.getCompletedQty().signum() > 0) {
            row.setUnitCost(total.divide(row.getCompletedQty(), 6, RoundingMode.HALF_UP));
        } else {
            row.setUnitCost(ZERO);
        }
        if (total.signum() > 0) {
            row.setMaterialRate(rate(row.getDoneMaterial(), total));
            row.setLaborRate(rate(row.getDoneLabor(), total));
            row.setOverheadRate(rate(row.getDoneOverhead(), total));
        } else {
            row.setMaterialRate(ZERO);
            row.setLaborRate(ZERO);
            row.setOverheadRate(ZERO);
        }
    }

    private static BigDecimal rate(BigDecimal part, BigDecimal total) {
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private YearMonth parseMonth(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw exception(COST_PERIOD_INVALID);
        }
    }

}
