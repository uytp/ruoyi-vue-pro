package cn.iocoder.yudao.module.fms.service.tax;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.FmsLedgerListReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.ledger.vo.subjectbalance.FmsLedgerSubjectBalanceRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.tax.vo.*;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherEntrySaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAccountSetDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsVoucherWordDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.tax.FmsInvoiceDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.tax.FmsTaxRecordDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.tax.FmsTaxSettingDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherDO;
import cn.iocoder.yudao.module.fms.dal.mysql.tax.FmsInvoiceMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.tax.FmsTaxRecordMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.tax.FmsTaxSettingMapper;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;

/**
 * FMS 发票台账与增值税 Service 实现
 *
 * 思路：发票台账登记每一张销项、进项发票；每月汇总成增值税申报表：
 * 销项税额 - 本月认证的进项专票税额 - 上期留抵 = 应纳税额，再算附加税，最后一键生成一张税务结转凭证。
 * 某个月生成了税务凭证后，会影响这个月的发票就不能再改（要先撤销凭证），免得凭证和发票对不上。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FmsTaxServiceImpl implements FmsTaxService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    /** 税额和“金额 × 税率”允许的误差（发票上经常有几分钱的尾差） */
    private static final BigDecimal TAX_TOLERANCE = new BigDecimal("0.10");
    /** 留抵滚动最多往前算多少个月 */
    private static final int MAX_MONTHS = 120;

    /** 应交增值税-转出未交增值税 */
    private static final String CODE_TRANSFER_OUT = "22210104";
    /** 应交税费-未交增值税 */
    private static final String CODE_UNPAID_VAT = "222102";
    /** 应交增值税-销项税额 */
    private static final String CODE_OUTPUT_TAX = "22210107";
    /** 应交增值税-进项税额 */
    private static final String CODE_INPUT_TAX = "22210101";
    /** 税金及附加-城市维护建设税 / 教育费附加 / 地方教育费附加 */
    private static final String[] SURCHARGE_EXPENSE_CODES = {"540303", "540310", "540313"};
    /** 应交城市维护建设税 / 应交教育费附加 / 应交地方教育费附加 */
    private static final String[] SURCHARGE_PAYABLE_CODES = {"222117", "222113", "222114"};
    private static final String[] SURCHARGE_NAMES = {"城市维护建设税", "教育费附加", "地方教育费附加"};

    @Resource
    private FmsInvoiceMapper invoiceMapper;
    @Resource
    private FmsTaxSettingMapper settingMapper;
    @Resource
    private FmsTaxRecordMapper recordMapper;
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

    // ==================== 发票台账 ====================

    @Override
    public List<FmsInvoiceRespVO> getInvoiceList(Long accountSetId, Integer direction, String month, Integer status,
            String keyword, Long userId) {
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        Set<String> lockedPeriods = lockedPeriods(accountSetId, userId);
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        List<FmsInvoiceRespVO> result = new ArrayList<>();
        for (FmsInvoiceDO invoice : invoiceMapper.selectListByAccountSetId(accountSetId)) {
            if (direction != null && !ObjUtil.equal(invoice.getDirection(), direction)) {
                continue;
            }
            if (status != null && !ObjUtil.equal(invoice.getStatus(), status)) {
                continue;
            }
            if (month != null && !month.isBlank() && !month.equals(monthOf(invoice.getInvoiceDate()))) {
                continue;
            }
            if (!key.isEmpty() && !(invoice.getInvoiceCode() + " " + invoice.getInvoiceNo() + " "
                    + invoice.getCounterpartName()).toLowerCase().contains(key)) {
                continue;
            }
            result.add(toResp(invoice, lockedPeriods));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveInvoice(FmsInvoiceSaveReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);

        // 1. 校验数字，补默认值
        int direction = reqVO.getDirection();
        if (direction != FmsInvoiceDO.DIRECTION_OUTPUT && direction != FmsInvoiceDO.DIRECTION_INPUT) {
            throw exception(INVOICE_VALUE_INVALID);
        }
        int type = reqVO.getInvoiceType() == null ? FmsInvoiceDO.TYPE_SPECIAL : reqVO.getInvoiceType();
        if (type != FmsInvoiceDO.TYPE_SPECIAL && type != FmsInvoiceDO.TYPE_NORMAL) {
            throw exception(INVOICE_VALUE_INVALID);
        }
        BigDecimal amount = reqVO.getAmount() == null ? ZERO : reqVO.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = reqVO.getTaxRate() == null ? BigDecimal.valueOf(-1) : reqVO.getTaxRate();
        if (amount.signum() == 0 || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw exception(INVOICE_VALUE_INVALID);
        }
        BigDecimal computedTax = FmsTaxCalculator.calcTax(amount, rate);
        BigDecimal tax = computedTax;
        if (reqVO.getTaxAmount() != null) {
            tax = reqVO.getTaxAmount().setScale(2, RoundingMode.HALF_UP);
            if (tax.subtract(computedTax).abs().compareTo(TAX_TOLERANCE) > 0) {
                throw exception(INVOICE_TAX_MISMATCH, computedTax.toPlainString());
            }
        }
        String code = reqVO.getInvoiceCode() == null ? "" : reqVO.getInvoiceCode().trim();
        String no = reqVO.getInvoiceNo().trim();

        // 2. 同一方向下 代码 + 号码 不能重复
        FmsInvoiceDO same = invoiceMapper.selectByUnique(accountSetId, direction, code, no);
        if (same != null && !ObjUtil.equal(same.getId(), reqVO.getId())) {
            throw exception(INVOICE_DUPLICATE, code.isEmpty() ? "（空）" : code, no);
        }

        // 3. 修改：已作废的不能改；认证信息尽量保留，但不再符合条件就清掉
        FmsInvoiceDO existing = reqVO.getId() == null ? null : validateInvoice(accountSetId, reqVO.getId());
        boolean resetCertify = false;
        if (existing != null) {
            if (ObjUtil.equal(existing.getStatus(), FmsInvoiceDO.STATUS_VOIDED)) {
                throw exception(INVOICE_VOIDED, existing.getInvoiceNo());
            }
            if (ObjUtil.equal(existing.getCertifyStatus(), 1)) {
                boolean stillSpecialInput = direction == FmsInvoiceDO.DIRECTION_INPUT && type == FmsInvoiceDO.TYPE_SPECIAL;
                boolean periodOk = existing.getCertifyPeriod() != null
                        && existing.getCertifyPeriod().compareTo(monthOf(reqVO.getInvoiceDate())) >= 0;
                resetCertify = !(stillSpecialInput && periodOk);
            }
        }

        FmsInvoiceDO invoice = new FmsInvoiceDO();
        invoice.setAccountSetId(accountSetId);
        invoice.setDirection(direction);
        invoice.setInvoiceType(type);
        invoice.setInvoiceCode(code);
        invoice.setInvoiceNo(no);
        invoice.setInvoiceDate(reqVO.getInvoiceDate());
        invoice.setCounterpartName(reqVO.getCounterpartName().trim());
        invoice.setCounterpartTaxNo(reqVO.getCounterpartTaxNo());
        invoice.setAmount(amount);
        invoice.setTaxRate(rate);
        invoice.setTaxAmount(tax);
        invoice.setTotalAmount(amount.add(tax));
        invoice.setRemark(reqVO.getRemark());

        // 4. 修改前后影响的月份，只要有一个月已经生成了税务凭证，就不让改
        Set<String> periods = new HashSet<>();
        if (existing != null) {
            periods.addAll(affectedPeriods(existing, false));
        }
        FmsInvoiceDO preview = new FmsInvoiceDO();
        preview.setDirection(direction);
        preview.setInvoiceType(type);
        preview.setInvoiceDate(reqVO.getInvoiceDate());
        preview.setStatus(FmsInvoiceDO.STATUS_NORMAL);
        preview.setCertifyStatus(existing == null || resetCertify ? 0 : existing.getCertifyStatus());
        preview.setCertifyPeriod(existing == null || resetCertify ? null : existing.getCertifyPeriod());
        periods.addAll(affectedPeriods(preview, false));
        assertNotLocked(accountSetId, periods, userId);

        if (existing == null) {
            invoice.setStatus(FmsInvoiceDO.STATUS_NORMAL);
            invoice.setCertifyStatus(0);
            invoiceMapper.insert(invoice);
        } else {
            invoice.setId(existing.getId());
            invoiceMapper.updateById(invoice);
            if (resetCertify) {
                clearCertify(existing.getId());
            }
        }
        return invoice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInvoice(Long accountSetId, Long id, Long userId) {
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        FmsInvoiceDO invoice = validateInvoice(accountSetId, id);
        assertNotLocked(accountSetId, affectedPeriods(invoice, false), userId);
        invoiceMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidInvoices(FmsInvoiceBatchReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        boolean toVoid = Boolean.TRUE.equals(reqVO.getFlag());
        // 先全部检查，再统一改，避免改了一半报错
        List<FmsInvoiceDO> invoices = new ArrayList<>();
        Set<String> periods = new HashSet<>();
        for (Long id : new LinkedHashSet<>(reqVO.getIds())) {
            FmsInvoiceDO invoice = validateInvoice(accountSetId, id);
            if (toVoid && ObjUtil.equal(invoice.getStatus(), FmsInvoiceDO.STATUS_VOIDED)) {
                throw exception(INVOICE_VOIDED, invoice.getInvoiceNo());
            }
            if (!toVoid && !ObjUtil.equal(invoice.getStatus(), FmsInvoiceDO.STATUS_VOIDED)) {
                throw exception(INVOICE_NOT_VOIDED, invoice.getInvoiceNo());
            }
            periods.addAll(affectedPeriods(invoice, true)); // 作废和恢复影响的是“正常时”算进去的月份
            invoices.add(invoice);
        }
        assertNotLocked(accountSetId, periods, userId);
        for (FmsInvoiceDO invoice : invoices) {
            FmsInvoiceDO update = new FmsInvoiceDO();
            update.setId(invoice.getId());
            update.setStatus(toVoid ? FmsInvoiceDO.STATUS_VOIDED : FmsInvoiceDO.STATUS_NORMAL);
            invoiceMapper.updateById(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void certifyInvoices(FmsInvoiceBatchReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        boolean certify = Boolean.TRUE.equals(reqVO.getFlag());
        Map<Long, String> newPeriods = new LinkedHashMap<>();
        List<FmsInvoiceDO> invoices = new ArrayList<>();
        Set<String> periods = new HashSet<>();
        for (Long id : new LinkedHashSet<>(reqVO.getIds())) {
            FmsInvoiceDO invoice = validateInvoice(accountSetId, id);
            if (!ObjUtil.equal(invoice.getStatus(), FmsInvoiceDO.STATUS_NORMAL)
                    || !ObjUtil.equal(invoice.getDirection(), FmsInvoiceDO.DIRECTION_INPUT)
                    || !ObjUtil.equal(invoice.getInvoiceType(), FmsInvoiceDO.TYPE_SPECIAL)) {
                throw exception(INVOICE_CERTIFY_NOT_APPLICABLE, invoice.getInvoiceNo());
            }
            periods.addAll(affectedPeriods(invoice, false)); // 原来已经认证的那个月
            if (certify) {
                String invoiceMonth = monthOf(invoice.getInvoiceDate());
                String period = reqVO.getCertifyPeriod() == null || reqVO.getCertifyPeriod().isBlank()
                        ? invoiceMonth : reqVO.getCertifyPeriod().trim();
                try {
                    YearMonth.parse(period);
                } catch (DateTimeParseException e) {
                    throw exception(INVOICE_CERTIFY_PERIOD_INVALID, invoiceMonth);
                }
                if (period.compareTo(invoiceMonth) < 0) {
                    throw exception(INVOICE_CERTIFY_PERIOD_INVALID, invoiceMonth);
                }
                periods.add(period);
                newPeriods.put(id, period);
            }
            invoices.add(invoice);
        }
        assertNotLocked(accountSetId, periods, userId);
        for (FmsInvoiceDO invoice : invoices) {
            if (certify) {
                FmsInvoiceDO update = new FmsInvoiceDO();
                update.setId(invoice.getId());
                update.setCertifyStatus(1);
                update.setCertifyPeriod(newPeriods.get(invoice.getId()));
                invoiceMapper.updateById(update);
            } else {
                clearCertify(invoice.getId());
            }
        }
    }

    // ==================== 税务参数 ====================

    @Override
    public FmsTaxSettingVO getSetting(Long accountSetId, Long userId) {
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        FmsTaxSettingDO setting = settingMapper.selectByAccountSetId(accountSetId);
        FmsTaxSettingVO vo = new FmsTaxSettingVO();
        vo.setAccountSetId(accountSetId);
        vo.setStartMonth(setting == null ? null : setting.getStartMonth());
        vo.setInitCarry(setting == null ? ZERO : setting.getInitCarry());
        vo.setUrbanRate(setting == null ? BigDecimal.valueOf(7) : setting.getUrbanRate());
        vo.setEduRate(setting == null ? BigDecimal.valueOf(3) : setting.getEduRate());
        vo.setLocalEduRate(setting == null ? BigDecimal.valueOf(2) : setting.getLocalEduRate());
        vo.setEffectiveStartMonth(effectiveStart(setting, accountSet).toString());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSetting(FmsTaxSettingVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal initCarry = reqVO.getInitCarry() == null ? ZERO : reqVO.getInitCarry().setScale(2, RoundingMode.HALF_UP);
        BigDecimal urban = reqVO.getUrbanRate() == null ? BigDecimal.valueOf(7) : reqVO.getUrbanRate();
        BigDecimal edu = reqVO.getEduRate() == null ? BigDecimal.valueOf(3) : reqVO.getEduRate();
        BigDecimal localEdu = reqVO.getLocalEduRate() == null ? BigDecimal.valueOf(2) : reqVO.getLocalEduRate();
        for (BigDecimal rate : Arrays.asList(urban, edu, localEdu)) {
            if (rate.signum() < 0 || rate.compareTo(hundred) > 0) {
                throw exception(TAX_SETTING_INVALID);
            }
        }
        String startMonth = reqVO.getStartMonth() == null || reqVO.getStartMonth().isBlank() ? null
                : reqVO.getStartMonth().trim();
        if (initCarry.signum() < 0) {
            throw exception(TAX_SETTING_INVALID);
        }
        if (startMonth != null) {
            try {
                YearMonth.parse(startMonth);
            } catch (DateTimeParseException e) {
                throw exception(TAX_SETTING_INVALID);
            }
        }
        FmsTaxSettingDO existing = settingMapper.selectByAccountSetId(accountSetId);
        if (existing == null) {
            FmsTaxSettingDO setting = new FmsTaxSettingDO();
            setting.setAccountSetId(accountSetId);
            setting.setStartMonth(startMonth);
            setting.setInitCarry(initCarry);
            setting.setUrbanRate(urban);
            setting.setEduRate(edu);
            setting.setLocalEduRate(localEdu);
            settingMapper.insert(setting);
        } else {
            // 开始月份允许清空，所以用 update wrapper
            settingMapper.update(null, new LambdaUpdateWrapper<FmsTaxSettingDO>()
                    .eq(FmsTaxSettingDO::getId, existing.getId())
                    .set(FmsTaxSettingDO::getStartMonth, startMonth)
                    .set(FmsTaxSettingDO::getInitCarry, initCarry)
                    .set(FmsTaxSettingDO::getUrbanRate, urban)
                    .set(FmsTaxSettingDO::getEduRate, edu)
                    .set(FmsTaxSettingDO::getLocalEduRate, localEdu));
        }
    }

    // ==================== 增值税申报 ====================

    @Override
    public FmsTaxVatRespVO getVat(Long accountSetId, String period, Long userId) {
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        YearMonth ym = parsePeriod(period);
        FmsTaxSettingDO setting = settingMapper.selectByAccountSetId(accountSetId);
        YearMonth start = effectiveStart(setting, accountSet);
        if (ym.isBefore(start)) {
            throw exception(TAX_PERIOD_BEFORE_START, period, start.toString());
        }
        if (ChronoUnit.MONTHS.between(start, ym) > MAX_MONTHS) {
            throw exception(TAX_PERIOD_TOO_FAR, period);
        }
        List<FmsInvoiceDO> invoices = invoiceMapper.selectListByAccountSetId(accountSetId);
        BigDecimal initCarry = setting == null ? ZERO : setting.getInitCarry();
        BigDecimal urban = setting == null ? BigDecimal.valueOf(7) : setting.getUrbanRate();
        BigDecimal edu = setting == null ? BigDecimal.valueOf(3) : setting.getEduRate();
        BigDecimal localEdu = setting == null ? BigDecimal.valueOf(2) : setting.getLocalEduRate();

        // 1. 从开始月份滚动到上个月，得到上期留抵
        BigDecimal carry = initCarry;
        for (YearMonth m = start; m.isBefore(ym); m = m.plusMonths(1)) {
            carry = FmsTaxCalculator.vat(sumOutputTax(invoices, m.toString()), sumInputTax(invoices, m.toString()), carry)
                    .carryEnd;
        }

        // 2. 本月
        FmsTaxVatRespVO resp = new FmsTaxVatRespVO();
        resp.setPeriod(period);
        fillRows(resp.getSalesRows(), invoices, period, true);
        fillRows(resp.getInputRows(), invoices, period, false);
        for (FmsTaxVatRespVO.RateRow row : resp.getSalesRows()) {
            resp.setSalesAmount(resp.getSalesAmount().add(row.getAmount()));
            resp.setOutputTax(resp.getOutputTax().add(row.getTaxAmount()));
        }
        for (FmsTaxVatRespVO.RateRow row : resp.getInputRows()) {
            resp.setInputTax(resp.getInputTax().add(row.getTaxAmount()));
        }
        String monthEnd = ym.toString();
        for (FmsInvoiceDO invoice : invoices) {
            if (isInputSpecial(invoice) && ObjUtil.equal(invoice.getCertifyStatus(), 0)
                    && monthOf(invoice.getInvoiceDate()).compareTo(monthEnd) <= 0) {
                resp.setUncertifiedCount(resp.getUncertifiedCount() + 1);
                resp.setUncertifiedTax(resp.getUncertifiedTax().add(invoice.getTaxAmount()));
            }
        }
        FmsTaxCalculator.Vat vat = FmsTaxCalculator.vat(resp.getOutputTax(), resp.getInputTax(), carry);
        resp.setPriorCarry(carry);
        resp.setNet(vat.net);
        resp.setPayable(vat.payable);
        resp.setCarryEnd(vat.carryEnd);

        // 3. 附加税
        Map<String, FmsSubjectDO> subjectByCode = getSubjectByCode(accountSetId, userId);
        BigDecimal[] rates = {urban, edu, localEdu};
        for (int i = 0; i < 3; i++) {
            FmsTaxVatRespVO.SurchargeRow row = new FmsTaxVatRespVO.SurchargeRow();
            row.setName(SURCHARGE_NAMES[i]);
            row.setRate(rates[i]);
            row.setBase(vat.payable);
            row.setAmount(FmsTaxCalculator.surcharge(vat.payable, rates[i]));
            row.setExpenseSubjectName(subjectLabel(subjectByCode, SURCHARGE_EXPENSE_CODES[i]));
            row.setPayableSubjectName(subjectLabel(subjectByCode, SURCHARGE_PAYABLE_CODES[i]));
            resp.getSurcharges().add(row);
            resp.setSurchargeTotal(resp.getSurchargeTotal().add(row.getAmount()));
        }
        resp.setTotalPayable(vat.payable.add(resp.getSurchargeTotal()));

        // 4. 是否已生成凭证；生成之后发票或参数又变了，要提醒
        FmsTaxRecordDO record = findRecord(accountSetId, period, userId, false);
        if (record != null) {
            resp.setGenerated(true);
            resp.setVoucherId(record.getVoucherId());
            resp.setVoucherNo(getVoucherNo(accountSetId, record.getVoucherId(), userId));
            if (record.getVatPayable().compareTo(vat.payable) != 0
                    || record.getSurchargeTotal().compareTo(resp.getSurchargeTotal()) != 0) {
                resp.getWarnings().add("税务凭证生成之后，发票或税务参数又有变化：凭证里是增值税 "
                        + record.getVatPayable().toPlainString() + "、附加税 " + record.getSurchargeTotal().toPlainString()
                        + "，按现在的数据算是 " + vat.payable.toPlainString() + "、"
                        + resp.getSurchargeTotal().toPlainString() + "。请撤销凭证后重新生成");
            }
        }
        if (resp.getUncertifiedCount() > 0) {
            resp.getWarnings().add("还有 " + resp.getUncertifiedCount() + " 张进项专票没有认证（税额合计 "
                    + resp.getUncertifiedTax().toPlainString() + "），认证后才能抵扣");
        }
        if (carry.signum() > 0) {
            resp.getWarnings().add("上期留抵税额 " + carry.toPlainString() + " 已经抵扣");
        }

        // 5. 与总账对数
        addReconcile(resp, accountSetId, period, userId, subjectByCode);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateTaxVoucher(FmsTaxPeriodReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        YearMonth ym = parsePeriod(reqVO.getPeriod());
        if (findRecord(accountSetId, reqVO.getPeriod(), userId, true) != null) {
            throw exception(TAX_GENERATED, reqVO.getPeriod());
        }
        FmsTaxVatRespVO vat = getVat(accountSetId, reqVO.getPeriod(), userId);
        Map<String, FmsSubjectDO> subjectByCode = getSubjectByCode(accountSetId, userId);
        List<FmsVoucherEntrySaveReqVO> entries = new ArrayList<>();
        // 1. 结转未交增值税：借 应交增值税-转出未交增值税，贷 未交增值税
        if (vat.getPayable().signum() > 0) {
            String digest = "结转" + reqVO.getPeriod() + "未交增值税";
            entries.add(entry(digest, requireSubject(subjectByCode, CODE_TRANSFER_OUT, "转出未交增值税").getId(),
                    vat.getPayable(), ZERO));
            entries.add(entry(digest, requireSubject(subjectByCode, CODE_UNPAID_VAT, "未交增值税").getId(),
                    ZERO, vat.getPayable()));
        }
        // 2. 计提附加税：借 税金及附加，贷 应交城建税 / 教育费附加 / 地方教育费附加
        for (int i = 0; i < 3; i++) {
            FmsTaxVatRespVO.SurchargeRow row = vat.getSurcharges().get(i);
            if (row.getAmount().signum() <= 0) {
                continue;
            }
            String digest = "计提" + reqVO.getPeriod() + SURCHARGE_NAMES[i];
            entries.add(entry(digest, requireSubject(subjectByCode, SURCHARGE_EXPENSE_CODES[i], "税金及附加-" + SURCHARGE_NAMES[i]).getId(),
                    row.getAmount(), ZERO));
            entries.add(entry(digest, requireSubject(subjectByCode, SURCHARGE_PAYABLE_CODES[i], "应交" + SURCHARGE_NAMES[i]).getId(),
                    ZERO, row.getAmount()));
        }
        if (entries.isEmpty()) {
            throw exception(TAX_NOTHING_TO_GENERATE, reqVO.getPeriod());
        }
        Long voucherId = createVoucher(accountSetId, ym.atEndOfMonth().atStartOfDay(), entries, userId);
        FmsTaxRecordDO record = new FmsTaxRecordDO();
        record.setAccountSetId(accountSetId);
        record.setPeriod(reqVO.getPeriod());
        record.setVatPayable(vat.getPayable());
        record.setSurchargeTotal(vat.getSurchargeTotal());
        record.setVoucherId(voucherId);
        recordMapper.insert(record);
        return voucherId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeTaxVoucher(FmsTaxPeriodReqVO reqVO, Long userId) {
        Long accountSetId = reqVO.getAccountSetId();
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        parsePeriod(reqVO.getPeriod());
        FmsTaxRecordDO record = findRecord(accountSetId, reqVO.getPeriod(), userId, true);
        if (record == null) {
            throw exception(TAX_NOT_GENERATED, reqVO.getPeriod());
        }
        deleteVoucherIfExists(accountSetId, record.getVoucherId(), userId);
        recordMapper.deleteById(record.getId());
    }

    // ==================== 内部方法 ====================

    private void fillRows(List<FmsTaxVatRespVO.RateRow> rows, List<FmsInvoiceDO> invoices, String period, boolean sales) {
        Map<BigDecimal, FmsTaxVatRespVO.RateRow> map = new TreeMap<>();
        for (FmsInvoiceDO invoice : invoices) {
            boolean hit = sales ? isSale(invoice, period) : isDeductible(invoice, period);
            if (!hit) {
                continue;
            }
            FmsTaxVatRespVO.RateRow row = map.computeIfAbsent(invoice.getTaxRate().setScale(2, RoundingMode.HALF_UP), rate -> {
                FmsTaxVatRespVO.RateRow created = new FmsTaxVatRespVO.RateRow();
                created.setTaxRate(rate);
                return created;
            });
            row.setCount(row.getCount() + 1);
            row.setAmount(row.getAmount().add(invoice.getAmount()));
            row.setTaxAmount(row.getTaxAmount().add(invoice.getTaxAmount()));
        }
        rows.addAll(map.values());
    }

    private BigDecimal sumOutputTax(List<FmsInvoiceDO> invoices, String period) {
        BigDecimal total = ZERO;
        for (FmsInvoiceDO invoice : invoices) {
            if (isSale(invoice, period)) {
                total = total.add(invoice.getTaxAmount());
            }
        }
        return total;
    }

    private BigDecimal sumInputTax(List<FmsInvoiceDO> invoices, String period) {
        BigDecimal total = ZERO;
        for (FmsInvoiceDO invoice : invoices) {
            if (isDeductible(invoice, period)) {
                total = total.add(invoice.getTaxAmount());
            }
        }
        return total;
    }

    /** 本月的销项：正常的销项发票，开票日期在本月 */
    private static boolean isSale(FmsInvoiceDO invoice, String period) {
        return ObjUtil.equal(invoice.getStatus(), FmsInvoiceDO.STATUS_NORMAL)
                && ObjUtil.equal(invoice.getDirection(), FmsInvoiceDO.DIRECTION_OUTPUT)
                && period.equals(monthOf(invoice.getInvoiceDate()));
    }

    private static boolean isInputSpecial(FmsInvoiceDO invoice) {
        return ObjUtil.equal(invoice.getStatus(), FmsInvoiceDO.STATUS_NORMAL)
                && ObjUtil.equal(invoice.getDirection(), FmsInvoiceDO.DIRECTION_INPUT)
                && ObjUtil.equal(invoice.getInvoiceType(), FmsInvoiceDO.TYPE_SPECIAL);
    }

    /** 本月可抵扣的进项：正常的进项专票，已认证，认证所属期是本月 */
    private static boolean isDeductible(FmsInvoiceDO invoice, String period) {
        return isInputSpecial(invoice) && ObjUtil.equal(invoice.getCertifyStatus(), 1)
                && period.equals(invoice.getCertifyPeriod());
    }

    /**
     * 这张发票会影响哪些月份的增值税：销项是开票月份，进项专票是认证所属期
     *
     * @param ignoreStatus true 表示不管它现在是不是作废（作废、恢复的时候看的是“正常时”影响哪个月）
     */
    private static Set<String> affectedPeriods(FmsInvoiceDO invoice, boolean ignoreStatus) {
        Set<String> periods = new HashSet<>();
        if (!ignoreStatus && !ObjUtil.equal(invoice.getStatus(), FmsInvoiceDO.STATUS_NORMAL)) {
            return periods;
        }
        if (ObjUtil.equal(invoice.getDirection(), FmsInvoiceDO.DIRECTION_OUTPUT)) {
            periods.add(monthOf(invoice.getInvoiceDate()));
        } else if (ObjUtil.equal(invoice.getDirection(), FmsInvoiceDO.DIRECTION_INPUT)
                && ObjUtil.equal(invoice.getInvoiceType(), FmsInvoiceDO.TYPE_SPECIAL)
                && ObjUtil.equal(invoice.getCertifyStatus(), 1) && invoice.getCertifyPeriod() != null) {
            periods.add(invoice.getCertifyPeriod());
        }
        return periods;
    }

    private void assertNotLocked(Long accountSetId, Set<String> periods, Long userId) {
        if (periods.isEmpty()) {
            return;
        }
        Set<String> locked = lockedPeriods(accountSetId, userId);
        for (String period : new TreeSet<>(periods)) {
            if (locked.contains(period)) {
                throw exception(INVOICE_PERIOD_LOCKED, period, period);
            }
        }
    }

    private Set<String> lockedPeriods(Long accountSetId, Long userId) {
        Set<String> periods = new HashSet<>();
        for (FmsTaxRecordDO record : loadRecords(accountSetId, userId, false)) {
            periods.add(record.getPeriod());
        }
        return periods;
    }

    private FmsTaxRecordDO findRecord(Long accountSetId, String period, Long userId, boolean heal) {
        for (FmsTaxRecordDO record : loadRecords(accountSetId, userId, heal)) {
            if (period.equals(record.getPeriod())) {
                return record;
            }
        }
        return null;
    }

    /**
     * 加载税务凭证记录。凭证已经被手工删除或作废的记录算失效：heal 为 true 时顺便物理删掉
     */
    private List<FmsTaxRecordDO> loadRecords(Long accountSetId, Long userId, boolean heal) {
        List<FmsTaxRecordDO> valid = new ArrayList<>();
        List<Long> staleIds = new ArrayList<>();
        for (FmsTaxRecordDO record : recordMapper.selectListByAccountSetId(accountSetId)) {
            if (isVoucherValid(accountSetId, record.getVoucherId(), userId)) {
                valid.add(record);
            } else {
                staleIds.add(record.getId());
            }
        }
        if (heal && !staleIds.isEmpty()) {
            recordMapper.deleteByIds(staleIds);
        }
        return valid;
    }

    private void addReconcile(FmsTaxVatRespVO resp, Long accountSetId, String period, Long userId,
            Map<String, FmsSubjectDO> subjectByCode) {
        Map<Long, FmsLedgerSubjectBalanceRespVO> ledgerMap = new HashMap<>();
        try {
            FmsLedgerListReqVO req = new FmsLedgerListReqVO();
            req.setAccountSetId(accountSetId);
            req.setStartMonth(period);
            req.setEndMonth(period);
            flattenBalance(ledgerService.getSubjectBalanceList(req, userId), ledgerMap);
        } catch (ServiceException e) {
            return; // 总账取不到数就不对了，不影响申报表
        }
        FmsSubjectDO output = subjectByCode.get(CODE_OUTPUT_TAX);
        if (output != null) {
            FmsLedgerSubjectBalanceRespVO node = ledgerMap.get(output.getId());
            BigDecimal ledger = node == null ? ZERO : nvl(node.getPeriodCreditAmount()).subtract(nvl(node.getPeriodDebitAmount()));
            resp.getReconcile().add(reconcileRow("销项税额", output, resp.getOutputTax(), ledger));
        }
        FmsSubjectDO input = subjectByCode.get(CODE_INPUT_TAX);
        if (input != null) {
            FmsLedgerSubjectBalanceRespVO node = ledgerMap.get(input.getId());
            BigDecimal ledger = node == null ? ZERO : nvl(node.getPeriodDebitAmount()).subtract(nvl(node.getPeriodCreditAmount()));
            resp.getReconcile().add(reconcileRow("可抵扣进项税额", input, resp.getInputTax(), ledger));
        }
    }

    private static FmsTaxVatRespVO.ReconcileRow reconcileRow(String name, FmsSubjectDO subject, BigDecimal invoice,
            BigDecimal ledger) {
        FmsTaxVatRespVO.ReconcileRow row = new FmsTaxVatRespVO.ReconcileRow();
        row.setName(name);
        row.setSubjectName(subject.getCode() + " " + subject.getName());
        row.setInvoiceAmount(invoice);
        row.setLedgerAmount(ledger);
        row.setDiff(invoice.subtract(ledger));
        return row;
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

    private FmsInvoiceRespVO toResp(FmsInvoiceDO invoice, Set<String> lockedPeriods) {
        FmsInvoiceRespVO vo = new FmsInvoiceRespVO();
        vo.setId(invoice.getId());
        vo.setDirection(invoice.getDirection());
        vo.setInvoiceType(invoice.getInvoiceType());
        vo.setInvoiceCode(invoice.getInvoiceCode());
        vo.setInvoiceNo(invoice.getInvoiceNo());
        vo.setInvoiceDate(invoice.getInvoiceDate());
        vo.setInvoiceMonth(monthOf(invoice.getInvoiceDate()));
        vo.setCounterpartName(invoice.getCounterpartName());
        vo.setCounterpartTaxNo(invoice.getCounterpartTaxNo());
        vo.setAmount(invoice.getAmount());
        vo.setTaxRate(invoice.getTaxRate());
        vo.setTaxAmount(invoice.getTaxAmount());
        vo.setTotalAmount(invoice.getTotalAmount());
        vo.setStatus(invoice.getStatus());
        vo.setCertifyStatus(invoice.getCertifyStatus());
        vo.setCertifyPeriod(invoice.getCertifyPeriod());
        // 作废的发票也要能恢复，所以“锁定”按“正常时影响的月份”判断
        Set<String> affected = affectedPeriods(invoice, true);
        vo.setLocked(affected.stream().anyMatch(lockedPeriods::contains));
        vo.setRemark(invoice.getRemark());
        return vo;
    }

    private void clearCertify(Long id) {
        invoiceMapper.update(null, new LambdaUpdateWrapper<FmsInvoiceDO>()
                .eq(FmsInvoiceDO::getId, id)
                .set(FmsInvoiceDO::getCertifyStatus, 0)
                .set(FmsInvoiceDO::getCertifyPeriod, null));
    }

    private FmsInvoiceDO validateInvoice(Long accountSetId, Long id) {
        FmsInvoiceDO invoice = id == null ? null : invoiceMapper.selectById(id);
        if (invoice == null || !ObjUtil.equal(invoice.getAccountSetId(), accountSetId)) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        return invoice;
    }

    private static YearMonth effectiveStart(FmsTaxSettingDO setting, FmsAccountSetDO accountSet) {
        YearMonth accountStart = YearMonth.from(accountSet.getStartTime());
        if (setting == null || setting.getStartMonth() == null || setting.getStartMonth().isBlank()) {
            return accountStart;
        }
        try {
            return YearMonth.parse(setting.getStartMonth());
        } catch (DateTimeParseException e) {
            return accountStart;
        }
    }

    private static String monthOf(LocalDate date) {
        return YearMonth.from(date).toString();
    }

    private static YearMonth parsePeriod(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw exception(TAX_PERIOD_INVALID);
        }
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private Map<String, FmsSubjectDO> getSubjectByCode(Long accountSetId, Long userId) {
        return convertMap(subjectService.getSubjectList(accountSetId, null, userId), FmsSubjectDO::getCode);
    }

    private static String subjectLabel(Map<String, FmsSubjectDO> subjectByCode, String code) {
        FmsSubjectDO subject = subjectByCode.get(code);
        return subject == null ? code + "（科目不存在）" : subject.getCode() + " " + subject.getName();
    }

    private static FmsSubjectDO requireSubject(Map<String, FmsSubjectDO> subjectByCode, String code, String name) {
        FmsSubjectDO subject = subjectByCode.get(code);
        if (subject == null) {
            throw exception(TAX_SUBJECT_MISSING, code, name);
        }
        return subject;
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

}
