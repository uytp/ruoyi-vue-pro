package cn.iocoder.yudao.module.fms.service.voucher;

import java.time.LocalDate;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAccountSetDO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherAttachmentUpdateReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherEntrySaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherImportExcelVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherImportRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherImportTemplateVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherMoveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherStatisticsReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherStatisticsRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherSubjectAmountVO;
import cn.iocoder.yudao.module.fms.controller.admin.voucher.vo.FmsVoucherTidyReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryCombinationDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryTypeDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsSubjectDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsVoucherWordDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.voucher.FmsVoucherEntryDO;
import cn.iocoder.yudao.module.fms.dal.mysql.voucher.FmsVoucherEntryMapper;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsCurrencyDO;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsCurrencyMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.module.fms.dal.mysql.voucher.FmsVoucherMapper;
import cn.iocoder.yudao.module.fms.enums.voucher.FmsVoucherStatusEnum;
import cn.iocoder.yudao.module.fms.service.closing.FmsClosingPeriodService;
import cn.iocoder.yudao.module.fms.service.closing.FmsClosingVoucherService;
import cn.iocoder.yudao.module.fms.service.config.FmsAccountSetService;
import cn.iocoder.yudao.module.fms.service.config.FmsAuxiliaryCombinationService;
import cn.iocoder.yudao.module.fms.service.config.FmsAuxiliaryItemService;
import cn.iocoder.yudao.module.fms.service.config.FmsAuxiliaryTypeService;
import cn.iocoder.yudao.module.fms.service.config.FmsSubjectService;
import cn.iocoder.yudao.module.fms.service.config.FmsVoucherWordService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.count;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.filterList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.sumBigDecimal;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.fms.enums.LogRecordConstants.*;
import static java.util.Collections.singletonList;

/**
 * FMS 凭证 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FmsVoucherServiceImpl implements FmsVoucherService {

    @Resource
    private FmsVoucherMapper voucherMapper;
    @Resource
    private FmsVoucherEntryMapper voucherEntryMapper;
    @Resource
    private FmsCurrencyMapper currencyMapper;

    @Resource
    private FmsAccountSetService accountSetService;
    @Resource
    @Lazy // 延迟加载，避免与结账 Service 循环依赖
    private FmsClosingPeriodService closingPeriodService;
    @Resource
    @Lazy // 延迟加载，避免与结转凭证 Service 循环依赖
    private FmsClosingVoucherService closingVoucherService;
    @Resource
    private FmsVoucherWordService voucherWordService;
    @Resource
    private FmsSubjectService subjectService;
    @Resource
    private FmsAuxiliaryItemService auxiliaryItemService;
    @Resource
    private FmsAuxiliaryTypeService auxiliaryTypeService;
    @Resource
    private FmsAuxiliaryCombinationService auxiliaryCombinationService;

    // ==================== 凭证写操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FMS_VOUCHER_TYPE, subType = FMS_VOUCHER_CREATE_SUB_TYPE,
            bizNo = "{{#voucherId}}", success = FMS_VOUCHER_CREATE_SUCCESS)
    public Long createVoucher(FmsVoucherSaveReqVO createReqVO, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(createReqVO.getAccountSetId(), userId);
        // 1.2 串行化同一账套的凭证编号写操作
        accountSetService.lockAccountSet(createReqVO.getAccountSetId());
        // 1.3 校验会计期间
        closingPeriodService.validatePeriodOpen(createReqVO.getAccountSetId(), createReqVO.getVoucherTime());
        // 1.4 校验凭证字
        voucherWordService.validateVoucherWordExists(createReqVO.getAccountSetId(), createReqVO.getVoucherWordId());
        // 1.5 校验凭证号唯一
        LocalDateTime voucherTime = normalizeVoucherTime(createReqVO.getVoucherTime());
        validateVoucherNumberUnique(null, createReqVO.getAccountSetId(), createReqVO.getVoucherWordId(),
                voucherTime, createReqVO.getVoucherNumber());

        // 2. 校验并构造凭证分录
        List<FmsVoucherEntryDO> entries = validateAndBuildEntries(createReqVO, userId);
        BigDecimal debitAmount = sumBigDecimal(entries, FmsVoucherEntryDO::getDebitAmount);
        BigDecimal creditAmount = sumBigDecimal(entries, FmsVoucherEntryDO::getCreditAmount);

        // 3. 创建凭证和分录
        // 3.1 创建凭证
        FmsVoucherDO voucher = BeanUtils.toBean(createReqVO, FmsVoucherDO.class)
                .setId(null).setVoucherTime(voucherTime).setAttachmentUrls(Collections.emptyList())
                .setDebitAmount(debitAmount).setCreditAmount(creditAmount)
                .setTotal(debitAmount)
                .setStatus(FmsVoucherStatusEnum.PENDING_REVIEW.getStatus());
        try {
            voucherMapper.insert(voucher);
        } catch (DuplicateKeyException ex) {
            // 兼容尚未移除凭证业务唯一索引的存量数据库
            throw exception(VOUCHER_NUMBER_DUPLICATE);
        }
        // 3.2 创建凭证分录
        entries.forEach(entry -> entry.setId(null).setVoucherId(voucher.getId()));
        entries.forEach(voucherEntryMapper::insert);

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("voucherId", voucher.getId());
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FMS_VOUCHER_TYPE, subType = FMS_VOUCHER_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = FMS_VOUCHER_UPDATE_SUCCESS)
    public void updateVoucher(FmsVoucherSaveReqVO updateReqVO, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(updateReqVO.getAccountSetId(), userId);
        // 1.2 串行化同一账套的凭证编号写操作
        accountSetService.lockAccountSet(updateReqVO.getAccountSetId());
        // 1.3 校验凭证可编辑
        FmsVoucherDO voucher = validateVoucherExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        validateClosingVoucherEditable(updateReqVO.getAccountSetId(), singletonList(voucher.getId()));
        validateVoucherEditable(voucher);
        if (ObjUtil.equal(voucher.getStatus(), FmsVoucherStatusEnum.VOIDED.getStatus())) {
            throw exception(VOUCHER_VOIDED_NOT_EDITABLE);
        }
        // 1.4 校验原会计期间和新会计期间
        closingPeriodService.validatePeriodOpen(updateReqVO.getAccountSetId(), voucher.getVoucherTime());
        closingPeriodService.validatePeriodOpen(updateReqVO.getAccountSetId(), updateReqVO.getVoucherTime());
        // 1.5 校验凭证字
        voucherWordService.validateVoucherWordExists(updateReqVO.getAccountSetId(), updateReqVO.getVoucherWordId());
        // 1.6 校验凭证号唯一
        LocalDateTime voucherTime = normalizeVoucherTime(updateReqVO.getVoucherTime());
        validateVoucherNumberUnique(voucher.getId(), updateReqVO.getAccountSetId(),
                updateReqVO.getVoucherWordId(), voucherTime, updateReqVO.getVoucherNumber());

        // 2. 校验并构造凭证分录
        List<FmsVoucherEntryDO> entries = validateAndBuildEntries(updateReqVO, userId);
        BigDecimal debitAmount = sumBigDecimal(entries, FmsVoucherEntryDO::getDebitAmount);
        BigDecimal creditAmount = sumBigDecimal(entries, FmsVoucherEntryDO::getCreditAmount);

        // 3.1 更新凭证
        FmsVoucherDO updateObj = BeanUtils.toBean(updateReqVO, FmsVoucherDO.class)
                .setVoucherTime(voucherTime).setAttachmentUrls(null)
                .setDebitAmount(debitAmount).setCreditAmount(creditAmount).setTotal(debitAmount);
        voucherMapper.updateById(updateObj);

        // 3.2 更新凭证分录
        updateVoucherEntryList(voucher.getId(), entries);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FMS_VOUCHER_TYPE, subType = FMS_VOUCHER_ATTACHMENT_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = FMS_VOUCHER_ATTACHMENT_UPDATE_SUCCESS)
    public void updateVoucherAttachments(FmsVoucherAttachmentUpdateReqVO updateReqVO, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(updateReqVO.getAccountSetId(), userId);
        // 1.2 校验凭证可编辑
        FmsVoucherDO voucher = validateVoucherExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        validateClosingVoucherEditable(updateReqVO.getAccountSetId(), singletonList(voucher.getId()));
        validateVoucherEditable(voucher);
        // 1.3 校验会计期间
        closingPeriodService.validatePeriodOpen(updateReqVO.getAccountSetId(), voucher.getVoucherTime());

        // 2. 更新附件
        List<String> attachmentUrls = ObjUtil.defaultIfNull(updateReqVO.getAttachmentUrls(), Collections.emptyList());
        voucherMapper.updateById(new FmsVoucherDO().setId(voucher.getId())
                .setAttachmentUrls(attachmentUrls));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FMS_VOUCHER_TYPE, subType = FMS_VOUCHER_DELETE_SUB_TYPE,
            bizNo = "{{#accountSetId}}", success = FMS_VOUCHER_DELETE_SUCCESS)
    public void deleteVoucherList(Long accountSetId, List<Long> ids, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        // 1.2 串行化同一账套的凭证编号写操作
        accountSetService.lockAccountSet(accountSetId);
        // 1.3 校验凭证可编辑
        List<FmsVoucherDO> vouchers = validateVoucherList(accountSetId, ids);
        validateClosingVoucherEditable(accountSetId, ids);
        vouchers.forEach(this::validateVoucherEditable);
        // 1.4 校验会计期间
        vouchers.forEach(voucher -> closingPeriodService.validatePeriodOpen(accountSetId, voucher.getVoucherTime()));

        // 2. 删除凭证和分录
        voucherEntryMapper.deleteByVoucherIds(ids);
        voucherMapper.deleteByIds(ids);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("voucher", CollUtil.getFirst(vouchers));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FMS_VOUCHER_TYPE, subType = FMS_VOUCHER_REVIEW_SUB_TYPE,
            bizNo = "{{#accountSetId}}", success = FMS_VOUCHER_REVIEW_SUCCESS)
    public void updateVoucherReviewStatus(
            Long accountSetId, List<Long> ids, Integer status, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        // 1.2 校验目标状态
        if (ObjectUtils.notEqualsAny(status, FmsVoucherStatusEnum.PENDING_REVIEW.getStatus(),
                FmsVoucherStatusEnum.APPROVED.getStatus())) {
            throw exception(VOUCHER_STATUS_INVALID);
        }
        // 1.3 校验凭证可审核
        List<FmsVoucherDO> vouchers = validateVoucherList(accountSetId, ids);
        validateClosingVoucherEditable(accountSetId, ids);
        // 1.4 校验会计期间
        vouchers.forEach(voucher -> closingPeriodService.validatePeriodOpen(
                accountSetId, voucher.getVoucherTime()));
        // 1.5 校验审核状态流转
        for (FmsVoucherDO voucher : vouchers) {
            boolean canApprove = ObjUtil.equal(status, FmsVoucherStatusEnum.APPROVED.getStatus())
                    && ObjUtil.equal(voucher.getStatus(), FmsVoucherStatusEnum.PENDING_REVIEW.getStatus());
            boolean canCancel = ObjUtil.equal(status, FmsVoucherStatusEnum.PENDING_REVIEW.getStatus())
                    && ObjUtil.equal(voucher.getStatus(), FmsVoucherStatusEnum.APPROVED.getStatus());
            if (!canApprove && !canCancel) {
                throw exception(VOUCHER_STATUS_INVALID);
            }
            if (canCancel && Boolean.TRUE.equals(voucher.getPosted())) {
                throw exception(VOUCHER_POSTED_NOT_UNREVIEWABLE);
            }
        }

        // 2. 更新审核状态和审核人
        Long reviewerUserId = ObjUtil.equal(status, FmsVoucherStatusEnum.APPROVED.getStatus()) ? userId : null;
        vouchers.forEach(voucher -> voucherMapper.updateReviewStatusById(
                voucher.getId(), status, reviewerUserId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVoucherPostStatus(Long accountSetId, List<Long> ids, Boolean posted, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        // 1.2 校验凭证存在，且不是结账生成凭证
        List<FmsVoucherDO> vouchers = validateVoucherList(accountSetId, ids);
        validateClosingVoucherEditable(accountSetId, ids);
        // 1.3 校验会计期间未结账
        vouchers.forEach(voucher -> closingPeriodService.validatePeriodOpen(
                accountSetId, voucher.getVoucherTime()));
        // 1.4 校验过账状态流转：已审核且未过账 -> 过账；已过账 -> 反过账
        boolean targetPosted = Boolean.TRUE.equals(posted);
        for (FmsVoucherDO voucher : vouchers) {
            boolean currentPosted = Boolean.TRUE.equals(voucher.getPosted());
            if (targetPosted) {
                if (ObjUtil.notEqual(voucher.getStatus(), FmsVoucherStatusEnum.APPROVED.getStatus())) {
                    throw exception(VOUCHER_POST_NOT_APPROVED);
                }
                if (currentPosted) {
                    throw exception(VOUCHER_POST_STATUS_INVALID);
                }
            } else if (!currentPosted) {
                throw exception(VOUCHER_POST_STATUS_INVALID);
            }
        }

        // 2. 更新过账状态、过账人和过账时间
        Long posterUserId = targetPosted ? userId : null;
        LocalDateTime postTime = targetPosted ? LocalDateTime.now() : null;
        vouchers.forEach(voucher -> voucherMapper.updatePostStatusById(
                voucher.getId(), targetPosted, posterUserId, postTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVoucherVoidStatus(Long accountSetId, List<Long> ids, Boolean voided, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        // 1.2 校验凭证存在，且不是结账生成凭证
        List<FmsVoucherDO> vouchers = validateVoucherList(accountSetId, ids);
        validateClosingVoucherEditable(accountSetId, ids);
        // 1.3 校验会计期间未结账
        vouchers.forEach(voucher -> closingPeriodService.validatePeriodOpen(
                accountSetId, voucher.getVoucherTime()));
        // 1.4 校验状态流转：待审核 -> 已作废；已作废 -> 待审核
        boolean targetVoided = Boolean.TRUE.equals(voided);
        Integer fromStatus = targetVoided ? FmsVoucherStatusEnum.PENDING_REVIEW.getStatus()
                : FmsVoucherStatusEnum.VOIDED.getStatus();
        Integer toStatus = targetVoided ? FmsVoucherStatusEnum.VOIDED.getStatus()
                : FmsVoucherStatusEnum.PENDING_REVIEW.getStatus();
        for (FmsVoucherDO voucher : vouchers) {
            if (ObjUtil.notEqual(voucher.getStatus(), fromStatus)) {
                throw exception(VOUCHER_VOID_STATUS_INVALID);
            }
        }

        // 2. 更新状态（作废凭证保留凭证号，账簿和报表不再计入）
        vouchers.forEach(voucher -> voucherMapper.updateReviewStatusById(voucher.getId(), toStatus, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reverseVoucher(Long accountSetId, Long id, Long userId) {
        // 1.1 校验账套写权限，并串行化凭证编号写操作
        FmsAccountSetDO accountSet = accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        accountSetService.lockAccountSet(accountSetId);
        // 1.2 校验原凭证：已审核、非结账生成、本身不是冲销凭证、未被冲销过
        FmsVoucherDO source = validateVoucherExists(accountSetId, id);
        if (ObjUtil.notEqual(source.getStatus(), FmsVoucherStatusEnum.APPROVED.getStatus())) {
            throw exception(VOUCHER_REVERSE_NOT_APPROVED);
        }
        validateClosingVoucherEditable(accountSetId, singletonList(id));
        if (source.getReversedVoucherId() != null) {
            throw exception(VOUCHER_REVERSE_SOURCE_INVALID);
        }
        if (CollUtil.isNotEmpty(voucherMapper.selectListByReversedVoucherIds(accountSetId, singletonList(id)))) {
            throw exception(VOUCHER_ALREADY_REVERSED);
        }

        // 2. 冲销凭证记在当前会计期间：今天在当前期间内用今天，否则用当前期间最后一天
        YearMonth currentMonth = closingPeriodService.getCurrentMonth(accountSetId, accountSet.getStartTime());
        LocalDate today = LocalDate.now();
        LocalDateTime voucherTime = YearMonth.from(today).equals(currentMonth)
                ? today.atStartOfDay() : currentMonth.atEndOfMonth().atStartOfDay();

        // 3. 按原分录生成红字分录：科目、辅助核算不变，金额和数量取负数
        FmsVoucherWordDO voucherWord = voucherWordService.getVoucherWordMap(accountSetId).get(source.getVoucherWordId());
        String prefix = "冲销" + (voucherWord == null ? "" : voucherWord.getName() + "-")
                + source.getVoucherNumber() + "号：";
        List<FmsVoucherEntrySaveReqVO> entries = new ArrayList<>();
        for (FmsVoucherEntryDO entry : voucherEntryMapper.selectListByVoucherId(id)) {
            String digest = prefix + (entry.getDigest() == null ? "" : entry.getDigest());
            FmsVoucherEntrySaveReqVO entryReqVO = new FmsVoucherEntrySaveReqVO();
            entryReqVO.setDigest(digest.length() > 500 ? digest.substring(0, 500) : digest);
            entryReqVO.setSubjectId(entry.getSubjectId());
            entryReqVO.setDebitAmount(entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount().negate());
            entryReqVO.setCreditAmount(entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount().negate());
            if (entry.getQuantity() != null && entry.getQuantity().signum() != 0) {
                entryReqVO.setQuantity(entry.getQuantity().negate());
                entryReqVO.setUnitPrice(entry.getUnitPrice());
            }
            List<FmsVoucherEntrySaveReqVO.AuxiliaryItem> auxiliaries = new ArrayList<>();
            if (entry.getAuxiliaries() != null) {
                for (FmsVoucherEntryDO.AuxiliaryItem item : entry.getAuxiliaries()) {
                    FmsVoucherEntrySaveReqVO.AuxiliaryItem auxiliary = new FmsVoucherEntrySaveReqVO.AuxiliaryItem();
                    auxiliary.setTypeId(item.getTypeId());
                    auxiliary.setItemId(item.getItemId());
                    auxiliaries.add(auxiliary);
                }
            }
            entryReqVO.setAuxiliaries(auxiliaries);
            if (entry.getCurrencyId() != null) {
                entryReqVO.setCurrencyId(entry.getCurrencyId());
                entryReqVO.setExchangeRate(entry.getExchangeRate());
                entryReqVO.setOriginalAmount(entry.getOriginalAmount() == null
                        ? BigDecimal.ZERO : entry.getOriginalAmount().negate());
            }
            entries.add(entryReqVO);
        }

        // 4. 创建冲销凭证，并记录对应的原凭证
        FmsVoucherSaveReqVO createReqVO = new FmsVoucherSaveReqVO();
        createReqVO.setAccountSetId(accountSetId);
        createReqVO.setVoucherWordId(source.getVoucherWordId());
        createReqVO.setVoucherTime(voucherTime);
        createReqVO.setVoucherNumber(getNextVoucherNumber(accountSetId, source.getVoucherWordId(), voucherTime, userId));
        createReqVO.setAttachmentCount(0);
        createReqVO.setEntries(entries);
        Long reverseVoucherId = createVoucher(createReqVO, userId);
        FmsVoucherDO updateObj = new FmsVoucherDO();
        updateObj.setId(reverseVoucherId);
        updateObj.setReversedVoucherId(id);
        voucherMapper.updateById(updateObj);
        return reverseVoucherId;
    }

    @Override
    public Set<Long> getReversedVoucherIdSet(Long accountSetId, Collection<Long> voucherIds) {
        if (CollUtil.isEmpty(voucherIds)) {
            return Collections.emptySet();
        }
        return convertSet(voucherMapper.selectListByReversedVoucherIds(accountSetId, voucherIds),
                FmsVoucherDO::getReversedVoucherId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FMS_VOUCHER_TYPE, subType = FMS_VOUCHER_TIDY_SUB_TYPE,
            bizNo = "{{#tidyReqVO.accountSetId}}", success = FMS_VOUCHER_TIDY_SUCCESS)
    public void tidyVoucher(FmsVoucherTidyReqVO tidyReqVO, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(tidyReqVO.getAccountSetId(), userId);
        // 1.2 串行化同一账套的凭证编号写操作
        accountSetService.lockAccountSet(tidyReqVO.getAccountSetId());
        // 1.3 校验会计期间
        YearMonth month = LocalDateTimeUtils.parseYearMonth(tidyReqVO.getMonth());
        LocalDateTime monthBeginTime = LocalDateTimeUtils.getMonthBeginTime(month);
        LocalDateTime nextMonthBeginTime = LocalDateTimeUtils.getNextMonthBeginTime(month);
        closingPeriodService.validatePeriodOpen(tidyReqVO.getAccountSetId(), monthBeginTime);
        // 1.4 校验凭证字
        if (tidyReqVO.getVoucherWordId() != null) {
            voucherWordService.validateVoucherWordExists(tidyReqVO.getAccountSetId(), tidyReqVO.getVoucherWordId());
        }

        // 2. 按凭证字和指定方式加载待整理凭证
        List<FmsVoucherDO> vouchers = voucherMapper.selectListForTidy(
                tidyReqVO.getAccountSetId(), monthBeginTime, nextMonthBeginTime, tidyReqVO.getVoucherWordId(),
                tidyReqVO.getStartNumber(), tidyReqVO.getType());
        if (CollUtil.isEmpty(vouchers)) {
            return;
        }
        Map<Long, List<FmsVoucherDO>> voucherMap = convertMultiMap(vouchers, FmsVoucherDO::getVoucherWordId);

        // 3. 按凭证字重新生成凭证号
        List<FmsVoucherDO> temporaryVouchers = new ArrayList<>();
        List<FmsVoucherDO> updateVouchers = new ArrayList<>();
        for (List<FmsVoucherDO> wordVouchers : voucherMap.values()) {
            for (int index = 0; index < wordVouchers.size(); index++) {
                FmsVoucherDO voucher = wordVouchers.get(index);
                temporaryVouchers.add(new FmsVoucherDO().setId(voucher.getId())
                        .setVoucherNumber(-index - 1));
                updateVouchers.add(new FmsVoucherDO().setId(voucher.getId())
                        .setVoucherNumber(tidyReqVO.getStartNumber() + index));
            }
        }
        // updateBatch 仍会逐条触发凭证号唯一键校验，直接写入最终号码可能与尚未更新的原号码冲突
        // 临时负数不会占用正常凭证号，可在同一事务内安全完成号码交换和重新排序
        voucherMapper.updateBatch(temporaryVouchers);
        voucherMapper.updateBatch(updateVouchers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FMS_VOUCHER_TYPE, subType = FMS_VOUCHER_MOVE_SUB_TYPE,
            bizNo = "{{#moveReqVO.accountSetId}}", success = FMS_VOUCHER_MOVE_SUCCESS)
    public void moveVoucher(FmsVoucherMoveReqVO moveReqVO, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(moveReqVO.getAccountSetId(), userId);
        // 1.2 串行化同一账套的凭证编号写操作
        accountSetService.lockAccountSet(moveReqVO.getAccountSetId());
        // 1.3 校验会计期间
        YearMonth month = LocalDateTimeUtils.parseYearMonth(moveReqVO.getMonth());
        LocalDateTime monthBeginTime = LocalDateTimeUtils.getMonthBeginTime(month);
        LocalDateTime nextMonthBeginTime = LocalDateTimeUtils.getNextMonthBeginTime(month);
        closingPeriodService.validatePeriodOpen(moveReqVO.getAccountSetId(), monthBeginTime);
        // 1.4 校验凭证字
        voucherWordService.validateVoucherWordExists(moveReqVO.getAccountSetId(), moveReqVO.getVoucherWordId());
        // 1.5 校验凭证编号范围
        if (moveReqVO.getTargetNumber() >= moveReqVO.getSourceNumber()) {
            throw exception(VOUCHER_MOVE_RANGE_INVALID);
        }

        // 2. 加载移动区间并校验原凭证
        List<FmsVoucherDO> vouchers = voucherMapper.selectListForMove(
                moveReqVO.getAccountSetId(), moveReqVO.getVoucherWordId(),
                monthBeginTime, nextMonthBeginTime,
                moveReqVO.getTargetNumber(), moveReqVO.getSourceNumber());
        FmsVoucherDO sourceVoucher = null;
        for (FmsVoucherDO voucher : vouchers) {
            if (ObjUtil.equal(voucher.getVoucherNumber(), moveReqVO.getSourceNumber())) {
                if (sourceVoucher != null) {
                    throw exception(VOUCHER_MOVE_SOURCE_NOT_EXISTS);
                }
                sourceVoucher = voucher;
            }
        }
        if (sourceVoucher == null) {
            throw exception(VOUCHER_MOVE_SOURCE_NOT_EXISTS);
        }

        // 3. 移动原凭证并顺延中间编号
        List<FmsVoucherDO> temporaryVouchers = new ArrayList<>();
        for (int index = 0; index < vouchers.size(); index++) {
            temporaryVouchers.add(new FmsVoucherDO().setId(vouchers.get(index).getId()).setVoucherNumber(-index - 1));
        }
        Long sourceVoucherId = sourceVoucher.getId();
        List<FmsVoucherDO> updateVouchers = convertList(vouchers, voucher -> new FmsVoucherDO().setId(voucher.getId())
                .setVoucherNumber(ObjUtil.equal(voucher.getId(), sourceVoucherId) ? moveReqVO.getTargetNumber() : voucher.getVoucherNumber() + 1));
        // updateBatch 仍会逐条触发凭证号唯一键校验，直接写入最终号码可能与尚未更新的原号码冲突
        // 临时负数不会占用正常凭证号，可在同一事务内安全完成号码移动和中间号码顺延
        voucherMapper.updateBatch(temporaryVouchers);
        voucherMapper.updateBatch(updateVouchers);
    }

    // ==================== 凭证查询 ====================

    @Override
    public FmsVoucherDO getVoucher(Long accountSetId, Long id, Long userId) {
        // 1. 校验账套读权限
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);

        // 2. 查询并校验凭证
        return validateVoucherExists(accountSetId, id);
    }

    @Override
    public PageResult<FmsVoucherDO> getVoucherPage(FmsVoucherPageReqVO pageReqVO, Long userId) {
        // 1. 校验账套读权限
        accountSetService.validateAccountSetReadPermission(pageReqVO.getAccountSetId(), userId);

        // 2. 查询凭证分页
        Collection<Long> voucherIds = null;
        if (pageReqVO.getSubjectId() != null || StrUtil.isNotBlank(pageReqVO.getDigest())
                || pageReqVO.getMinAmount() != null || pageReqVO.getMaxAmount() != null) {
            List<Long> subjectIds = pageReqVO.getSubjectId() == null ? null
                    : subjectService.getSubjectIdListWithChildren(pageReqVO.getAccountSetId(), pageReqVO.getSubjectId());
            List<FmsVoucherEntryDO> matchedEntries = voucherEntryMapper.selectVoucherIdsByCondition(
                    pageReqVO.getAccountSetId(), subjectIds, pageReqVO.getDigest(),
                    pageReqVO.getMinAmount(), pageReqVO.getMaxAmount());
            voucherIds = convertSet(matchedEntries, FmsVoucherEntryDO::getVoucherId);
            if (CollUtil.isEmpty(voucherIds)) {
                return PageResult.empty();
            }
        }
        return voucherMapper.selectPage(pageReqVO, voucherIds);
    }

    @Override
    public List<FmsVoucherDO> getVoucherListByPeriod(Long accountSetId,
            LocalDateTime beginTime, LocalDateTime endTime) {
        return voucherMapper.selectListByAccountSetIdAndVoucherTimeBetween(accountSetId, beginTime, endTime);
    }

    @Override
    public List<FmsVoucherEntryDO> getVoucherEntryList(Collection<Long> voucherIds) {
        if (CollUtil.isEmpty(voucherIds)) {
            return Collections.emptyList();
        }
        return voucherEntryMapper.selectListByVoucherIds(voucherIds);
    }

    @Override
    public List<FmsVoucherSubjectAmountVO> getVoucherSubjectAmountList(Collection<Long> voucherIds) {
        if (CollUtil.isEmpty(voucherIds)) {
            return Collections.emptyList();
        }
        return voucherEntryMapper.selectSubjectAmountListByVoucherIds(voucherIds);
    }

    @Override
    public Long getVoucherEntryCountBySubjectIds(Long accountSetId, Collection<Long> subjectIds) {
        return voucherEntryMapper.selectCountByAccountSetIdAndSubjectIds(accountSetId, subjectIds);
    }

    @Override
    public Long getVoucherEntryQuantityCountBySubjectIds(Long accountSetId, Collection<Long> subjectIds) {
        return voucherEntryMapper.selectQuantityCountByAccountSetIdAndSubjectIds(accountSetId, subjectIds);
    }

    @Override
    public void updateVoucherEntrySubject(Long accountSetId, Long subjectId, FmsSubjectDO targetSubject) {
        voucherEntryMapper.updateSubject(accountSetId, subjectId,
                new FmsVoucherEntryDO().setSubjectId(targetSubject.getId())
                        .setSubjectCode(targetSubject.getCode()).setSubjectName(targetSubject.getName()));
    }

    @Override
    public void migrateVoucherEntryAuxiliaries(Long accountSetId, Long subjectId,
            FmsAuxiliaryCombinationDO combination) {
        // 1. 查询科目的凭证分录
        List<FmsVoucherEntryDO> entries = voucherEntryMapper.selectListByAccountSetIdAndSubjectId(
                accountSetId, subjectId);
        if (CollUtil.isEmpty(entries)) {
            return;
        }

        // 2. 将历史分录迁移到指定辅助核算组合
        List<FmsVoucherEntryDO.AuxiliaryItem> auxiliaries = convertList(combination.getItems(), item ->
                FmsVoucherEntryDO.AuxiliaryItem.builder().type(item.getType()).typeId(item.getTypeId())
                        .itemId(item.getItemId()).name(item.getName()).build());
        entries.forEach(entry -> entry.setAssistCombinationId(combination.getId()).setAuxiliaries(auxiliaries));
        voucherEntryMapper.updateBatch(entries);
    }

    @Override
    public Long getVoucherEntryCountByAuxiliaryItemIds(Long accountSetId, Collection<Long> auxiliaryItemIds) {
        if (CollUtil.isEmpty(auxiliaryItemIds)) {
            return 0L;
        }
        Set<Long> auxiliaryItemIdSet = new LinkedHashSet<>(auxiliaryItemIds);
        return getVoucherEntryCountByAuxiliary(accountSetId,
                item -> auxiliaryItemIdSet.contains(item.getItemId()));
    }

    @Override
    public Long getVoucherEntryCountByAuxiliaryTypeId(Long accountSetId, Long auxiliaryTypeId) {
        return getVoucherEntryCountByAuxiliary(accountSetId,
                item -> ObjUtil.equal(item.getTypeId(), auxiliaryTypeId));
    }

    @Override
    public Long getVoucherCountByVoucherWordId(Long accountSetId, Long voucherWordId) {
        return voucherMapper.selectCountByAccountSetIdAndVoucherWordId(accountSetId, voucherWordId);
    }

    @Override
    public Integer getNextVoucherNumber(Long accountSetId, Long voucherWordId,
            LocalDateTime voucherTime, Long userId) {
        // 1.1 校验账套读权限
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);
        // 1.2 校验凭证字
        voucherWordService.validateVoucherWordExists(accountSetId, voucherWordId);

        // 2. 计算下一个凭证号
        return calculateNextVoucherNumber(accountSetId, voucherWordId, voucherTime);
    }

    // ==================== 凭证导入 ====================

    @Override
    public FmsVoucherImportTemplateVO getVoucherImportTemplateData(Long accountSetId, Long userId) {
        // 1. 校验账套读权限
        accountSetService.validateAccountSetReadPermission(accountSetId, userId);

        // 2. 加载导入模板依赖
        List<FmsVoucherWordDO> voucherWords = voucherWordService.getVoucherWordList(accountSetId);
        List<FmsSubjectDO> subjects = subjectService.getSubjectList(accountSetId, null, userId);
        List<FmsAuxiliaryTypeDO> auxiliaryTypes = auxiliaryTypeService.getAuxiliaryTypeList(accountSetId, userId);
        List<FmsAuxiliaryItemDO> auxiliaryItems = auxiliaryItemService.getAuxiliaryItemListByAccountSetId(accountSetId, userId);
        return new FmsVoucherImportTemplateVO(voucherWords, subjects, auxiliaryTypes, auxiliaryItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FmsVoucherImportRespVO importVoucher(Long accountSetId, List<FmsVoucherImportExcelVO> rows, Long userId) {
        // 1.1 校验账套写权限
        accountSetService.validateAccountSetWritePermission(accountSetId, userId);
        // 1.2 校验导入数据非空
        if (CollUtil.isEmpty(rows)) {
            throw exception(VOUCHER_IMPORT_FILE_EMPTY);
        }
        // 1.3 加载导入依赖
        Map<String, FmsVoucherWordDO> voucherWordMap = voucherWordService.getVoucherWordMapByName(accountSetId);
        List<FmsSubjectDO> subjects = subjectService.getSubjectList(accountSetId, null, userId);
        Map<Long, FmsAuxiliaryTypeDO> auxiliaryTypeMap = auxiliaryTypeService.getAuxiliaryTypeMap(accountSetId, userId);
        Map<String, FmsAuxiliaryItemDO> auxiliaryItemMap = auxiliaryItemService.getAuxiliaryItemMapByTypeIdAndCode(accountSetId, userId);
        Map<String, FmsSubjectDO> subjectMap = convertMap(subjects, FmsSubjectDO::getCode);
        Set<Long> parentSubjectIds = convertSet(subjects, FmsSubjectDO::getParentId);

        // 2. 按日期、凭证字和凭证号分组并校验每条分录
        Map<String, List<FmsVoucherImportExcelVO>> groupMap = new LinkedHashMap<>();
        for (FmsVoucherImportExcelVO row : rows) {
            validateImportRow(row, voucherWordMap, subjectMap, parentSubjectIds, auxiliaryTypeMap, auxiliaryItemMap);
            groupMap.computeIfAbsent(buildImportGroupKey(row), key -> new ArrayList<>()).add(row);
        }

        // 3. 校验凭证组摘要、期间、借贷平衡和业务键
        Set<String> importBusinessKeys = new LinkedHashSet<>();
        for (List<FmsVoucherImportExcelVO> groupRows : groupMap.values()) {
            validateImportGroup(accountSetId, groupRows, voucherWordMap);
            String businessKey = buildImportBusinessKey(CollUtil.getFirst(groupRows));
            if (businessKey != null && !importBusinessKeys.add(businessKey)) {
                addGroupError(groupRows, "同一会计期间、凭证字和凭证号重复");
            }
        }

        // 4. 落库有效凭证组
        int successVoucherCount = 0;
        List<FmsVoucherImportExcelVO> errorRows = new ArrayList<>();
        for (List<FmsVoucherImportExcelVO> groupRows : groupMap.values()) {
            if (groupRows.stream().anyMatch(row -> CollUtil.isNotEmpty(row.getErrors()))) {
                errorRows.addAll(groupRows);
                continue;
            }
            insertImportVoucher(accountSetId, groupRows, voucherWordMap, subjectMap, auxiliaryTypeMap, auxiliaryItemMap);
            successVoucherCount++;
        }
        return new FmsVoucherImportRespVO().setTotalRowCount(rows.size())
                .setSuccessRowCount(rows.size() - errorRows.size()).setFailureRowCount(errorRows.size())
                .setTotalVoucherCount(groupMap.size()).setSuccessVoucherCount(successVoucherCount)
                .setFailureVoucherCount(groupMap.size() - successVoucherCount).setErrorRows(errorRows);
    }

    // ==================== 凭证汇总 ====================

    @Override
    public List<FmsVoucherStatisticsRespVO> getVoucherStatisticsList(
            FmsVoucherStatisticsReqVO queryReqVO, Long userId) {
        // 1.1 校验账套读权限
        accountSetService.validateAccountSetReadPermission(queryReqVO.getAccountSetId(), userId);
        // 1.2 校验会计期间
        YearMonth startMonth = LocalDateTimeUtils.parseYearMonth(queryReqVO.getStartMonth());
        YearMonth endMonth = LocalDateTimeUtils.parseYearMonth(queryReqVO.getEndMonth());
        if (startMonth.isAfter(endMonth)) {
            throw exception(LEDGER_PERIOD_INVALID);
        }
        // 1.3 校验科目层级和凭证号范围
        int minLevel = ObjUtil.defaultIfNull(queryReqVO.getMinLevel(), 1);
        int maxLevel = ObjUtil.defaultIfNull(queryReqVO.getMaxLevel(), 1);
        if (minLevel > maxLevel || queryReqVO.getMinVoucherNumber() != null
                && queryReqVO.getMaxVoucherNumber() != null
                && queryReqVO.getMinVoucherNumber() > queryReqVO.getMaxVoucherNumber()) {
            throw exception(VOUCHER_STATISTICS_RANGE_INVALID);
        }
        // 1.4 校验凭证字
        if (queryReqVO.getVoucherWordId() != null) {
            voucherWordService.validateVoucherWordExists(
                    queryReqVO.getAccountSetId(), queryReqVO.getVoucherWordId());
        }

        // 2. 查询科目直接发生额
        LocalDateTime beginTime = LocalDateTimeUtils.getMonthBeginTime(startMonth);
        LocalDateTime endTime = LocalDateTimeUtils.getNextMonthBeginTime(endMonth);
        List<FmsVoucherSubjectAmountVO> amounts = voucherEntryMapper.selectSubjectAmountList(
                queryReqVO.getAccountSetId(), beginTime, endTime, queryReqVO.getVoucherWordId(),
                queryReqVO.getMinVoucherNumber(), queryReqVO.getMaxVoucherNumber());
        Map<Long, FmsVoucherSubjectAmountVO> amountMap = convertMap(
                amounts, FmsVoucherSubjectAmountVO::getSubjectId);
        if (CollUtil.isEmpty(amountMap)) {
            return new ArrayList<>();
        }

        // 3. 按科目级次汇总自身和全部下级发生额
        List<FmsSubjectDO> subjects = subjectService.getSubjectList(queryReqVO.getAccountSetId(), null, userId);
        Map<Long, List<Long>> childMap = convertMultiMap(
                filterList(subjects, subject -> subject.getParentId() != null),
                FmsSubjectDO::getParentId, FmsSubjectDO::getId);
        List<FmsVoucherStatisticsRespVO> result = new ArrayList<>();
        for (FmsSubjectDO subject : subjects) {
            if (subject.getLevel() < minLevel || subject.getLevel() > maxLevel) {
                continue;
            }
            Set<Long> subjectIds = new LinkedHashSet<>();
            Deque<Long> pendingSubjectIds = new ArrayDeque<>();
            pendingSubjectIds.add(subject.getId());
            while (CollUtil.isNotEmpty(pendingSubjectIds)) {
                Long subjectId = pendingSubjectIds.removeFirst();
                if (subjectIds.add(subjectId)) {
                    pendingSubjectIds.addAll(CollUtil.emptyIfNull(childMap.get(subjectId)));
                }
            }
            BigDecimal debitAmount = BigDecimal.ZERO;
            BigDecimal creditAmount = BigDecimal.ZERO;
            boolean hasAmount = false;
            for (Long subjectId : subjectIds) {
                FmsVoucherSubjectAmountVO amount = amountMap.get(subjectId);
                if (amount != null) {
                    debitAmount = debitAmount.add(amount.getDebitAmount());
                    creditAmount = creditAmount.add(amount.getCreditAmount());
                    hasAmount = true;
                }
            }
            if (hasAmount) {
                result.add(new FmsVoucherStatisticsRespVO().setSubjectId(subject.getId())
                        .setSubjectCode(subject.getCode()).setSubjectName(subject.getName())
                        .setLevel(subject.getLevel()).setDebitAmount(debitAmount).setCreditAmount(creditAmount));
            }
        }
        return result;
    }

    /**
     * 校验凭证导入行
     *
     * @param row 导入行
     * @param voucherWordMap 凭证字 Map
     * @param subjectMap 科目 Map
     * @param parentSubjectIds 非末级科目编号数组
     * @param auxiliaryTypeMap 辅助核算类别 Map
     * @param auxiliaryItemMap 辅助核算项目 Map
     */
    private void validateImportRow(FmsVoucherImportExcelVO row,
                                   Map<String, FmsVoucherWordDO> voucherWordMap, Map<String, FmsSubjectDO> subjectMap,
                                   Set<Long> parentSubjectIds, Map<Long, FmsAuxiliaryTypeDO> auxiliaryTypeMap,
                                   Map<String, FmsAuxiliaryItemDO> auxiliaryItemMap) {
        // 1. 校验 Excel 字段格式
        try {
            ValidationUtils.validate(row);
        } catch (ConstraintViolationException exception) {
            row.addError(exception.getMessage());
        }

        // 2. 校验凭证字和科目
        if (StrUtil.isNotBlank(row.getVoucherWordName())
                && !voucherWordMap.containsKey(row.getVoucherWordName())) {
            row.addError("凭证字不存在");
        }
        FmsSubjectDO subject = subjectMap.get(row.getSubjectCode());
        if (subject == null) {
            row.addError("科目编码不存在");
        } else if (ObjUtil.notEqual(subject.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            row.addError("科目已停用");
        } else if (parentSubjectIds.contains(subject.getId())) {
            row.addError("只能导入末级科目");
        }
        // 3. 校验借贷金额、数量核算和辅助核算
        BigDecimal debitAmount = normalizeAmount(row.getDebitAmount());
        BigDecimal creditAmount = normalizeAmount(row.getCreditAmount());
        if ((debitAmount.signum() != 0 && creditAmount.signum() != 0)
                || (debitAmount.signum() == 0 && creditAmount.signum() == 0)) {
            row.addError("借方金额和贷方金额只能填写一项且金额不能为 0");
        }
        if (subject != null) {
            validateImportQuantity(row, subject, debitAmount.abs().max(creditAmount.abs()));
            validateImportAuxiliaries(row, subject, auxiliaryTypeMap, auxiliaryItemMap);
        }
    }

    private void validateImportQuantity(
            FmsVoucherImportExcelVO row, FmsSubjectDO subject, BigDecimal amount) {
        BigDecimal quantity = ObjUtil.defaultIfNull(row.getQuantity(), BigDecimal.ZERO);
        BigDecimal unitPrice = ObjUtil.defaultIfNull(row.getUnitPrice(), BigDecimal.ZERO);
        if (Boolean.FALSE.equals(subject.getQuantityAccounting())) {
            if (quantity.signum() > 0 || unitPrice.signum() > 0) {
                row.addError("该科目未启用数量核算，不能填写数量和单价");
            }
            return;
        }
        if (quantity.signum() == 0 && unitPrice.signum() == 0) {
            return;
        }
        if (quantity.signum() <= 0 || unitPrice.signum() <= 0
                || quantity.multiply(unitPrice).setScale(2, RoundingMode.FLOOR).compareTo(amount) != 0) {
            row.addError("数量和单价需同时大于 0，且乘积应等于分录金额");
        }
    }

    /**
     * 校验凭证导入行的辅助核算项目
     *
     * @param row 导入行
     * @param subject 科目
     * @param auxiliaryTypeMap 辅助核算类别 Map
     * @param auxiliaryItemMap 辅助核算项目 Map
     */
    private void validateImportAuxiliaries(FmsVoucherImportExcelVO row, FmsSubjectDO subject,
                                           Map<Long, FmsAuxiliaryTypeDO> auxiliaryTypeMap,
                                           Map<String, FmsAuxiliaryItemDO> auxiliaryItemMap) {
        List<Long> auxiliaryTypeIds = ObjUtil.defaultIfNull(subject.getAuxiliaryTypeIds(), Collections.emptyList());
        // 按科目要求的辅助核算类别逐项校验项目编码和启用状态
        for (Long auxiliaryTypeId : auxiliaryTypeIds) {
            String code = row.getAuxiliaryCodes().get(auxiliaryTypeId);
            FmsAuxiliaryItemDO item = auxiliaryItemMap.get(buildAuxiliaryItemKey(auxiliaryTypeId, code));
            String typeName = auxiliaryTypeMap.get(auxiliaryTypeId).getName();
            if (StrUtil.isBlank(code) || item == null
                    || ObjUtil.notEqual(item.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
                row.addError(typeName + "辅助核算项目不存在或未填写");
            }
        }
    }

    /**
     * 校验同一凭证的导入分录
     *
     * @param accountSetId 账套编号
     * @param groupRows 同一凭证的导入行数组
     * @param voucherWordMap 凭证字 Map
     */
    private void validateImportGroup(Long accountSetId, List<FmsVoucherImportExcelVO> groupRows,
                                     Map<String, FmsVoucherWordDO> voucherWordMap) {
        FmsVoucherImportExcelVO firstRow = CollUtil.getFirst(groupRows);
        // 1. 校验分录数量、摘要和借贷平衡
        if (groupRows.size() < 2) {
            addGroupError(groupRows, "凭证至少需要两条分录");
        }
        if (groupRows.stream().noneMatch(row -> StrUtil.isNotBlank(row.getDigest()))) {
            addGroupError(groupRows, "同一凭证至少填写一条摘要");
        }
        BigDecimal debitAmount = sumBigDecimal(groupRows, row -> normalizeAmount(row.getDebitAmount()));
        BigDecimal creditAmount = sumBigDecimal(groupRows, row -> normalizeAmount(row.getCreditAmount()));
        if (debitAmount.compareTo(creditAmount) != 0) {
            addGroupError(groupRows, "凭证借贷金额不平衡");
        }
        // 2. 校验会计期间和凭证号唯一
        if (firstRow.getVoucherTime() == null) {
            return;
        }
        if (closingPeriodService.isPeriodClosed(accountSetId, firstRow.getVoucherTime())) {
            addGroupError(groupRows, "凭证所在会计期间已结账");
        }
        FmsVoucherWordDO voucherWord = voucherWordMap.get(firstRow.getVoucherWordName());
        if (voucherWord != null && firstRow.getVoucherNumber() != null
                && firstRow.getVoucherNumber() > 0) {
            YearMonth month = YearMonth.from(firstRow.getVoucherTime());
            LocalDateTime monthBeginTime = LocalDateTimeUtils.getMonthBeginTime(month);
            LocalDateTime nextMonthBeginTime = LocalDateTimeUtils.getNextMonthBeginTime(month);
            FmsVoucherDO voucher = voucherMapper.selectByAccountSetIdAndVoucherWordIdAndVoucherNumberAndVoucherTimeBetween(
                    accountSetId, voucherWord.getId(), firstRow.getVoucherNumber(),
                    monthBeginTime, nextMonthBeginTime);
            if (voucher != null) {
                addGroupError(groupRows, "同一会计期间、凭证字和凭证号已存在");
            }
        }
    }

    /**
     * 创建导入凭证及其分录
     *
     * @param accountSetId 账套编号
     * @param groupRows 同一凭证的导入行数组
     * @param voucherWordMap 凭证字 Map
     * @param subjectMap 科目 Map
     * @param auxiliaryTypeMap 辅助核算类别 Map
     * @param auxiliaryItemMap 辅助核算项目 Map
     */
    private void insertImportVoucher(Long accountSetId, List<FmsVoucherImportExcelVO> groupRows,
                                     Map<String, FmsVoucherWordDO> voucherWordMap, Map<String, FmsSubjectDO> subjectMap,
                                     Map<Long, FmsAuxiliaryTypeDO> auxiliaryTypeMap,
                                     Map<String, FmsAuxiliaryItemDO> auxiliaryItemMap) {
        FmsVoucherImportExcelVO firstRow = CollUtil.getFirst(groupRows);
        // 1. 构造凭证分录和辅助核算快照
        List<FmsVoucherEntryDO> entries = new ArrayList<>();
        for (int index = 0; index < groupRows.size(); index++) {
            FmsVoucherImportExcelVO row = groupRows.get(index);
            FmsSubjectDO subject = subjectMap.get(row.getSubjectCode());
            List<FmsVoucherEntryDO.AuxiliaryItem> auxiliaries = new ArrayList<>();
            List<Long> auxiliaryTypeIds = ObjUtil.defaultIfNull(subject.getAuxiliaryTypeIds(), Collections.emptyList());
            for (Long auxiliaryTypeId : auxiliaryTypeIds) {
                FmsAuxiliaryItemDO item = auxiliaryItemMap.get(
                        buildAuxiliaryItemKey(auxiliaryTypeId, row.getAuxiliaryCodes().get(auxiliaryTypeId)));
                auxiliaries.add(FmsVoucherEntryDO.AuxiliaryItem.builder()
                        .type(auxiliaryTypeMap.get(auxiliaryTypeId).getType())
                        .typeId(auxiliaryTypeId).itemId(item.getId())
                        .name(item.getName()).build());
            }
            BigDecimal rowDebitAmount = normalizeAmount(row.getDebitAmount());
            BigDecimal rowCreditAmount = normalizeAmount(row.getCreditAmount());
            entries.add(new FmsVoucherEntryDO().setAccountSetId(accountSetId)
                    .setDigest(row.getDigest()).setSubjectId(subject.getId())
                    .setSubjectCode(subject.getCode()).setSubjectName(subject.getName())
                    .setQuantity(ObjUtil.defaultIfNull(row.getQuantity(), BigDecimal.ZERO))
                    .setUnitPrice(ObjUtil.defaultIfNull(row.getUnitPrice(), BigDecimal.ZERO))
                    .setDebitAmount(rowDebitAmount).setCreditAmount(rowCreditAmount)
                    .setSort(index + 1).setAuxiliaries(auxiliaries));
        }
        auxiliaryCombinationService.setAuxiliaryCombinationIds(accountSetId, entries);

        // 2.1 创建凭证
        BigDecimal debitAmount = sumBigDecimal(entries, FmsVoucherEntryDO::getDebitAmount);
        BigDecimal creditAmount = sumBigDecimal(entries, FmsVoucherEntryDO::getCreditAmount);
        FmsVoucherDO voucher = new FmsVoucherDO().setAccountSetId(accountSetId)
                .setVoucherWordId(voucherWordMap.get(firstRow.getVoucherWordName()).getId())
                .setVoucherNumber(firstRow.getVoucherNumber()).setVoucherTime(normalizeVoucherTime(firstRow.getVoucherTime()))
                .setAttachmentUrls(new ArrayList<>())
                .setAttachmentCount(ObjUtil.defaultIfNull(firstRow.getAttachmentCount(), 0))
                .setDebitAmount(debitAmount).setCreditAmount(creditAmount)
                .setTotal(debitAmount)
                .setStatus(FmsVoucherStatusEnum.PENDING_REVIEW.getStatus());
        voucherMapper.insert(voucher);

        // 2.2 创建凭证分录
        entries.forEach(entry -> entry.setVoucherId(voucher.getId()));
        entries.forEach(voucherEntryMapper::insert);
    }

    private String buildImportGroupKey(FmsVoucherImportExcelVO row) {
        if (row.getVoucherTime() == null || StrUtil.isBlank(row.getVoucherWordName())
                || row.getVoucherNumber() == null) {
            return "第" + row.getRowNumber() + "行";
        }
        return row.getVoucherTime() + "|" + row.getVoucherWordName() + "|" + row.getVoucherNumber();
    }

    private String buildImportBusinessKey(FmsVoucherImportExcelVO row) {
        if (row.getVoucherTime() == null || StrUtil.isBlank(row.getVoucherWordName())
                || row.getVoucherNumber() == null) {
            return null;
        }
        return YearMonth.from(row.getVoucherTime()) + "|" + row.getVoucherWordName() + "|" + row.getVoucherNumber();
    }

    private String buildAuxiliaryItemKey(Long auxiliaryTypeId, String code) {
        return auxiliaryTypeId + "|" + code;
    }

    private void addGroupError(List<FmsVoucherImportExcelVO> rows, String error) {
        rows.forEach(row -> row.addError(error));
    }

    /**
     * 计算指定会计期间和凭证字的下一个凭证号
     *
     * @param accountSetId 账套编号
     * @param voucherWordId 凭证字编号
     * @param voucherTime 凭证日期
     * @return 下一个凭证号
     */
    private Integer calculateNextVoucherNumber(
            Long accountSetId, Long voucherWordId, LocalDateTime voucherTime) {
        YearMonth month = YearMonth.from(voucherTime);
        LocalDateTime monthBeginTime = LocalDateTimeUtils.getMonthBeginTime(month);
        LocalDateTime nextMonthBeginTime = LocalDateTimeUtils.getNextMonthBeginTime(month);
        FmsVoucherDO lastVoucher = voucherMapper.selectLastByPeriod(
                accountSetId, voucherWordId, monthBeginTime, nextMonthBeginTime);
        return lastVoucher == null ? 1 : lastVoucher.getVoucherNumber() + 1;
    }

    /**
     * 校验并构造凭证分录
     *
     * @param reqVO 凭证保存信息
     * @param userId 用户编号
     * @return 凭证分录数组
     */
    private List<FmsVoucherEntryDO> validateAndBuildEntries(FmsVoucherSaveReqVO reqVO, Long userId) {
        if (CollUtil.size(reqVO.getEntries()) < 2) {
            throw exception(VOUCHER_ENTRY_REQUIRED);
        }

        // 1. 批量加载科目和辅助核算项目
        List<FmsSubjectDO> allSubjects = subjectService.getSubjectList(reqVO.getAccountSetId(), null, userId);
        Map<Long, FmsSubjectDO> subjectMap = convertMap(allSubjects, FmsSubjectDO::getId);
        Set<Long> parentIds = convertSet(allSubjects, FmsSubjectDO::getParentId);
        Set<Long> auxiliaryTypeIds = convertSetByFlatMap(
                allSubjects, FmsSubjectDO::getAuxiliaryTypeIds, Collection::stream);
        List<FmsAuxiliaryTypeDO> auxiliaryTypes = auxiliaryTypeService.validateAuxiliaryTypeList(
                reqVO.getAccountSetId(), auxiliaryTypeIds);
        Map<Long, FmsAuxiliaryTypeDO> auxiliaryTypeMap = convertMap(auxiliaryTypes, FmsAuxiliaryTypeDO::getId);
        Set<FmsVoucherEntrySaveReqVO.AuxiliaryItem> auxiliaryItems = convertSetByFlatMap(
                reqVO.getEntries(), FmsVoucherEntrySaveReqVO::getAuxiliaries, Collection::stream);
        Set<Long> auxiliaryItemIds = convertSet(auxiliaryItems, FmsVoucherEntrySaveReqVO.AuxiliaryItem::getItemId);
        List<FmsAuxiliaryItemDO> auxiliaryItemList = auxiliaryItemService.validateAuxiliaryItemList(
                reqVO.getAccountSetId(), auxiliaryItemIds);
        Map<Long, FmsAuxiliaryItemDO> auxiliaryItemMap = convertMap(auxiliaryItemList, FmsAuxiliaryItemDO::getId);
        Map<Long, FmsCurrencyDO> currencyMap = convertMap(
                currencyMapper.selectListByAccountSetId(reqVO.getAccountSetId()), FmsCurrencyDO::getId);

        // 2. 校验并构造每条分录
        List<FmsVoucherEntryDO> entries = new ArrayList<>();
        for (int index = 0; index < reqVO.getEntries().size(); index++) {
            FmsVoucherEntrySaveReqVO entryReqVO = reqVO.getEntries().get(index);
            FmsSubjectDO subject = subjectMap.get(entryReqVO.getSubjectId());
            validateVoucherSubject(subject, parentIds);
            BigDecimal debit = normalizeAmount(entryReqVO.getDebitAmount());
            BigDecimal credit = normalizeAmount(entryReqVO.getCreditAmount());
            if ((debit.signum() != 0 && credit.signum() != 0)
                    || (debit.signum() == 0 && credit.signum() == 0)) {
                throw exception(VOUCHER_ENTRY_AMOUNT_INVALID);
            }
            validateQuantity(entryReqVO, subject, debit.signum() != 0 ? debit : credit);
            List<FmsVoucherEntryDO.AuxiliaryItem> auxiliaries = buildAuxiliaries(
                    entryReqVO, subject, auxiliaryTypeMap, auxiliaryItemMap);
            // 数量和单价仅在科目启用数量核算且实际填写时必填，未启用时保持零值
            entries.add(applyEntryCurrency(new FmsVoucherEntryDO().setAccountSetId(reqVO.getAccountSetId())
                    .setId(entryReqVO.getId())
                    .setDigest(entryReqVO.getDigest()).setSubjectId(subject.getId())
                    .setSubjectCode(subject.getCode()).setSubjectName(subject.getName())
                    .setQuantity(ObjUtil.defaultIfNull(entryReqVO.getQuantity(), BigDecimal.ZERO))
                    .setUnitPrice(ObjUtil.defaultIfNull(entryReqVO.getUnitPrice(), BigDecimal.ZERO))
                    .setDebitAmount(debit).setCreditAmount(credit).setSort(index + 1)
                    .setAuxiliaries(auxiliaries), entryReqVO, subject,
                    debit.signum() != 0 ? debit : credit, currencyMap));
        }

        // 3. 校验凭证借贷平衡并补充辅助核算组合编号
        BigDecimal debitAmount = sumBigDecimal(entries, FmsVoucherEntryDO::getDebitAmount);
        BigDecimal creditAmount = sumBigDecimal(entries, FmsVoucherEntryDO::getCreditAmount);
        if (debitAmount.compareTo(creditAmount) != 0) {
            throw exception(VOUCHER_AMOUNT_UNBALANCED);
        }
        auxiliaryCombinationService.setAuxiliaryCombinationIds(reqVO.getAccountSetId(), entries);
        return entries;
    }

    /**
     * 差量更新凭证分录
     *
     * @param voucherId 凭证编号
     * @param newEntries 新凭证分录数组
     */
    private void updateVoucherEntryList(Long voucherId, List<FmsVoucherEntryDO> newEntries) {
        // 1. 对比新老分录，获得新增、修改、删除列表
        List<FmsVoucherEntryDO> oldEntries = voucherEntryMapper.selectListByVoucherId(voucherId);
        List<List<FmsVoucherEntryDO>> diffEntries = diffList(oldEntries, newEntries,
                (oldEntry, newEntry) -> ObjUtil.equal(oldEntry.getId(), newEntry.getId()));

        // 2. 新增、修改、删除分录
        if (CollUtil.isNotEmpty(diffEntries.get(0))) {
            diffEntries.get(0).forEach(entry -> entry.setId(null).setVoucherId(voucherId));
            diffEntries.get(0).forEach(voucherEntryMapper::insert);
        }
        if (CollUtil.isNotEmpty(diffEntries.get(1))) {
            diffEntries.get(1).forEach(entry -> entry.setVoucherId(voucherId));
            voucherEntryMapper.updateBatch(diffEntries.get(1));
            // updateById 会忽略 null 字段：改成本位币分录时，显式清空原来的外币币别
            List<Long> localEntryIds = convertList(filterList(diffEntries.get(1),
                    entry -> entry.getCurrencyId() == null), FmsVoucherEntryDO::getId);
            if (CollUtil.isNotEmpty(localEntryIds)) {
                voucherEntryMapper.update(null, new LambdaUpdateWrapper<FmsVoucherEntryDO>()
                        .set(FmsVoucherEntryDO::getCurrencyId, null)
                        .in(FmsVoucherEntryDO::getId, localEntryIds));
            }
        }
        if (CollUtil.isNotEmpty(diffEntries.get(2))) {
            voucherEntryMapper.deleteByIds(convertList(diffEntries.get(2), FmsVoucherEntryDO::getId));
        }
    }

    /**
     * 校验并填充分录的外币信息
     *
     * 1. 科目未启用外币核算：不能选择币别，汇率、原币按 0 保存
     * 2. 科目启用外币核算：必须选择科目允许的币别、汇率大于 0；
     *    原币不为 0 时，本位币金额 = 原币 × 汇率（允许 0.01 尾差），原币与本位币正负号一致；
     *    原币为 0 的分录视为期末调汇 / 尾差调整分录
     */
    private FmsVoucherEntryDO applyEntryCurrency(FmsVoucherEntryDO entry, FmsVoucherEntrySaveReqVO entryReqVO,
            FmsSubjectDO subject, BigDecimal amount, Map<Long, FmsCurrencyDO> currencyMap) {
        List<Long> subjectCurrencyIds = subject.getCurrencyIds();
        if (CollUtil.isEmpty(subjectCurrencyIds)) {
            if (entryReqVO.getCurrencyId() != null) {
                FmsCurrencyDO currency = currencyMap.get(entryReqVO.getCurrencyId());
                if (currency == null || !Boolean.TRUE.equals(currency.getStandard())) {
                    throw exception(VOUCHER_CURRENCY_NOT_ALLOWED, subject.getName());
                }
            }
            return entry.setCurrencyId(null).setExchangeRate(BigDecimal.ZERO).setOriginalAmount(BigDecimal.ZERO);
        }
        Long currencyId = entryReqVO.getCurrencyId();
        if (currencyId == null) {
            throw exception(VOUCHER_CURRENCY_REQUIRED, subject.getName());
        }
        if (!subjectCurrencyIds.contains(currencyId) || !currencyMap.containsKey(currencyId)) {
            throw exception(VOUCHER_CURRENCY_NOT_ALLOWED, subject.getName());
        }
        BigDecimal rate = entryReqVO.getExchangeRate();
        if (rate == null || rate.signum() <= 0) {
            throw exception(VOUCHER_EXCHANGE_RATE_INVALID, subject.getName());
        }
        BigDecimal original = normalizeAmount(ObjUtil.defaultIfNull(entryReqVO.getOriginalAmount(), BigDecimal.ZERO));
        if (original.signum() != 0) {
            BigDecimal expected = original.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            if (original.signum() != amount.signum()
                    || expected.subtract(amount).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw exception(VOUCHER_ORIGINAL_AMOUNT_MISMATCH, subject.getName());
            }
        }
        return entry.setCurrencyId(currencyId).setExchangeRate(rate.setScale(6, RoundingMode.HALF_UP))
                .setOriginalAmount(original);
    }

    private void validateVoucherSubject(FmsSubjectDO subject, Set<Long> parentIds) {
        if (subject == null) {
            throw exception(SUBJECT_NOT_EXISTS);
        }
        if (ObjUtil.notEqual(subject.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            throw exception(VOUCHER_SUBJECT_DISABLED);
        }
        if (parentIds.contains(subject.getId())) {
            throw exception(VOUCHER_SUBJECT_HAS_CHILDREN);
        }
    }

    /**
     * 校验分录数量和单价
     *
     * @param entryReqVO 分录保存信息
     * @param subject 科目
     * @param amount 分录金额
     */
    private void validateQuantity(FmsVoucherEntrySaveReqVO entryReqVO,
            FmsSubjectDO subject, BigDecimal amount) {
        BigDecimal quantity = entryReqVO.getQuantity();
        BigDecimal unitPrice = entryReqVO.getUnitPrice();
        // 未启用数量核算的科目不能填写数量和单价
        if (Boolean.FALSE.equals(subject.getQuantityAccounting())) {
            if (quantity != null && quantity.signum() != 0 || unitPrice != null && unitPrice.signum() > 0) {
                throw exception(VOUCHER_QUANTITY_INVALID);
            }
            return;
        }
        // 数量和单价均未填写时，不进行数量金额校验
        if ((quantity == null || quantity.signum() == 0)
                && (unitPrice == null || unitPrice.signum() == 0)) {
            return;
        }
        // 数量和单价需同时填写且单价大于零；红字（负数金额）分录的数量为负数，其余为正数；数量×单价必须等于分录金额
        boolean red = amount.signum() < 0;
        if (quantity == null || unitPrice == null || unitPrice.signum() <= 0
                || (red ? quantity.signum() >= 0 : quantity.signum() <= 0)
                || quantity.abs().multiply(unitPrice).setScale(2, RoundingMode.FLOOR).compareTo(amount.abs()) != 0) {
            throw exception(VOUCHER_QUANTITY_INVALID);
        }
    }

    /**
     * 校验并构造分录辅助核算项目快照
     *
     * @param entryReqVO 分录保存信息
     * @param subject 科目
     * @param auxiliaryTypeMap 辅助核算类别 Map
     * @param auxiliaryItemMap 辅助核算项目 Map
     * @return 辅助核算项目快照数组
     */
    private List<FmsVoucherEntryDO.AuxiliaryItem> buildAuxiliaries(FmsVoucherEntrySaveReqVO entryReqVO, FmsSubjectDO subject,
                                                                   Map<Long, FmsAuxiliaryTypeDO> auxiliaryTypeMap,
                                                                   Map<Long, FmsAuxiliaryItemDO> auxiliaryItemMap) {
        List<Long> auxiliaryTypeIds = ObjUtil.defaultIfNull(subject.getAuxiliaryTypeIds(), Collections.emptyList());
        List<FmsVoucherEntrySaveReqVO.AuxiliaryItem> requested = ObjUtil.defaultIfNull(entryReqVO.getAuxiliaries(), Collections.emptyList());
        Map<Long, FmsVoucherEntrySaveReqVO.AuxiliaryItem> requestedMap = convertMap(
                requested, FmsVoucherEntrySaveReqVO.AuxiliaryItem::getTypeId);
        if (auxiliaryTypeIds.size() != requested.size() || requestedMap.size() != requested.size()) {
            throw exception(VOUCHER_AUXILIARY_REQUIRED);
        }

        // 按科目配置顺序校验项目归属和启用状态，并保存名称快照
        List<FmsVoucherEntryDO.AuxiliaryItem> auxiliaries = new ArrayList<>();
        for (Long auxiliaryTypeId : auxiliaryTypeIds) {
            FmsVoucherEntrySaveReqVO.AuxiliaryItem requestItem = requestedMap.get(auxiliaryTypeId);
            FmsAuxiliaryItemDO item = requestItem == null ? null
                    : auxiliaryItemMap.get(requestItem.getItemId());
            if (item == null || ObjUtil.notEqual(item.getAuxiliaryTypeId(), auxiliaryTypeId)
                    || ObjUtil.notEqual(item.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
                throw exception(VOUCHER_AUXILIARY_REQUIRED);
            }
            auxiliaries.add(FmsVoucherEntryDO.AuxiliaryItem.builder()
                    .type(auxiliaryTypeMap.get(auxiliaryTypeId).getType())
                    .typeId(auxiliaryTypeId).itemId(item.getId())
                    .name(item.getName()).build());
        }
        return auxiliaries;
    }

    private List<FmsVoucherDO> validateVoucherList(Long accountSetId, List<Long> ids) {
        List<FmsVoucherDO> vouchers = voucherMapper.selectListByIdsAndAccountSetId(ids, accountSetId);
        if (vouchers.size() != ids.size()) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        return vouchers;
    }

    private FmsVoucherDO validateVoucherExists(Long accountSetId, Long id) {
        FmsVoucherDO voucher = voucherMapper.selectById(id);
        if (voucher == null || ObjUtil.notEqual(voucher.getAccountSetId(), accountSetId)) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        return voucher;
    }

    private Long getVoucherEntryCountByAuxiliary(Long accountSetId,
                                                 Predicate<FmsVoucherEntryDO.AuxiliaryItem> predicate) {
        List<FmsVoucherEntryDO> entries = voucherEntryMapper.selectListByAccountSetId(accountSetId);
        return count(entries, entry -> CollUtil.isNotEmpty(entry.getAuxiliaries())
                && entry.getAuxiliaries().stream().anyMatch(predicate));
    }

    private void validateVoucherEditable(FmsVoucherDO voucher) {
        if (ObjUtil.equal(voucher.getStatus(), FmsVoucherStatusEnum.APPROVED.getStatus())) {
            throw exception(VOUCHER_APPROVED_NOT_EDITABLE);
        }
    }

    private void validateClosingVoucherEditable(Long accountSetId, Collection<Long> voucherIds) {
        if (CollUtil.isNotEmpty(closingVoucherService.getClosingVoucherIdSet(accountSetId, voucherIds))) {
            throw exception(VOUCHER_CLOSING_GENERATED_NOT_EDITABLE);
        }
    }

    private void validateVoucherNumberUnique(Long id, Long accountSetId, Long voucherWordId,
            LocalDateTime voucherTime, Integer voucherNumber) {
        YearMonth month = YearMonth.from(voucherTime);
        LocalDateTime monthBeginTime = LocalDateTimeUtils.getMonthBeginTime(month);
        LocalDateTime nextMonthBeginTime = LocalDateTimeUtils.getNextMonthBeginTime(month);
        FmsVoucherDO voucher = voucherMapper.selectByAccountSetIdAndVoucherWordIdAndVoucherNumberAndVoucherTimeBetween(
                accountSetId, voucherWordId, voucherNumber,
                monthBeginTime, nextMonthBeginTime);
        if (voucher != null && ObjUtil.notEqual(voucher.getId(), id)) {
            throw exception(VOUCHER_NUMBER_DUPLICATE);
        }
    }

    private LocalDateTime normalizeVoucherTime(LocalDateTime voucherTime) {
        return LocalDateTimeUtils.beginOfDay(voucherTime);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return MoneyUtils.priceScale(amount);
    }

}
