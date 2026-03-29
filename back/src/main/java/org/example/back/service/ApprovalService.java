package org.example.back.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.dto.ApprovalCreateDTO;
import org.example.back.dto.ApprovalDecisionDTO;
import org.example.back.dto.ApprovalQueryDTO;
import org.example.back.dto.DocumentVoidDTO;
import org.example.back.dto.LoginResponse;
import org.example.back.entity.BizApprovalOrder;
import org.example.back.entity.BizPurchase;
import org.example.back.entity.BizPurchaseReturn;
import org.example.back.entity.BizSales;
import org.example.back.entity.BizSalesReturn;
import org.example.back.mapper.BizApprovalOrderMapper;
import org.example.back.mapper.BizPurchaseMapper;
import org.example.back.mapper.BizPurchaseReturnMapper;
import org.example.back.mapper.BizSalesMapper;
import org.example.back.mapper.BizSalesReturnMapper;
import org.example.back.vo.ApprovalOrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ApprovalService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_PROCESSING = 4;

    private static final String ACTION_VOID = "void";
    private static final String ACTION_VOID_RED = "void_red";

    private static final String ROLE_EMPLOYEE = "employee";
    private static final String ROLE_ADMIN = "admin";
    private static final int APPROVAL_TEXT_MAX_LEN = 200;

    private static final DateTimeFormatter APPROVAL_NO_TIME_FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    @Autowired
    private BizApprovalOrderMapper bizApprovalOrderMapper;

    @Autowired
    private BizPurchaseMapper bizPurchaseMapper;

    @Autowired
    private BizPurchaseReturnMapper bizPurchaseReturnMapper;

    @Autowired
    private BizSalesMapper bizSalesMapper;

    @Autowired
    private BizSalesReturnMapper bizSalesReturnMapper;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private PurchaseReturnService purchaseReturnService;

    @Autowired
    private SalesService salesService;

    @Autowired
    private SalesReturnService salesReturnService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthzService authzService;

    @Transactional(rollbackFor = Exception.class)
    public void create(ApprovalCreateDTO dto) {
        LoginResponse.UserInfoVO requester = requireLoginUser();
        String role = normalizeRole(requester.getRole());

        String bizType = normalizeText(dto.getBizType());
        String action = normalizeText(dto.getRequestAction());
        validateBizType(bizType);
        validateAction(action);
        ensureRequesterCanSubmitApproval(requester, role, bizType);

        BizDocumentMeta meta = resolveBizMeta(bizType, dto.getBizId());
        ensureCanSubmitVoidApproval(meta);
        ensureNoPendingApproval(bizType, dto.getBizId());

        BizApprovalOrder entity = new BizApprovalOrder();
        entity.setApprovalNo(generateApprovalNo());
        entity.setBizType(bizType);
        entity.setBizId(dto.getBizId());
        entity.setBizNo(meta.bizNo());
        entity.setRequestAction(action);
        entity.setRequestReason(normalizeAndValidateLength("申请原因", defaultReason(dto.getReason()), APPROVAL_TEXT_MAX_LEN));
        entity.setBeforeBizStatus(meta.bizStatus());
        entity.setBeforeBizSnapshot(meta.snapshot());
        entity.setAfterBizStatus(meta.bizStatus());
        entity.setAfterBizSnapshot(meta.snapshot());
        entity.setStatus(STATUS_PENDING);
        entity.setRequesterId(requester.getId());
        entity.setRequesterName(requester.getRealName());
        entity.setRequesterRole(role);
        try {
            bizApprovalOrderMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw BusinessException.validateFail("该单据已存在待审批申请，请勿重复提交");
        }
    }

    public PageResult<ApprovalOrderVO> page(ApprovalQueryDTO queryDTO) {
        requireApprovalModuleAccess();
        String approvalNoKeyword = trimText(queryDTO.getApprovalNo()).toUpperCase(Locale.ROOT);

        LambdaQueryWrapper<BizApprovalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(approvalNoKeyword), BizApprovalOrder::getApprovalNo, approvalNoKeyword)
                .eq(StringUtils.hasText(queryDTO.getBizType()), BizApprovalOrder::getBizType, normalizeText(queryDTO.getBizType()))
                .eq(StringUtils.hasText(queryDTO.getRequestAction()), BizApprovalOrder::getRequestAction, normalizeText(queryDTO.getRequestAction()))
                .eq(queryDTO.getStatus() != null, BizApprovalOrder::getStatus, queryDTO.getStatus())
                .like(StringUtils.hasText(queryDTO.getRequesterName()), BizApprovalOrder::getRequesterName, queryDTO.getRequesterName())
                .orderByDesc(BizApprovalOrder::getCreateTime)
                .orderByDesc(BizApprovalOrder::getId);

        Page<BizApprovalOrder> page = bizApprovalOrderMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<ApprovalOrderVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public Long pendingCount() {
        requireApprovalModuleAccess();
        LambdaQueryWrapper<BizApprovalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizApprovalOrder::getStatus, STATUS_PENDING);
        return bizApprovalOrderMapper.selectCount(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ApprovalDecisionDTO dto) {
        LoginResponse.UserInfoVO approver = requireApprovalModuleAccess();
        BizApprovalOrder entity = claimPendingOrder(id, approver);
        BizDocumentMeta beforeMeta = resolveBizMeta(entity.getBizType(), entity.getBizId());

        entity.setBeforeBizStatus(beforeMeta.bizStatus());
        entity.setBeforeBizSnapshot(beforeMeta.snapshot());

        executeVoidByApproval(entity);
        BizDocumentMeta afterMeta = resolveBizMeta(entity.getBizType(), entity.getBizId());

        finalizeApprove(entity.getId(), dto, beforeMeta, afterMeta);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, ApprovalDecisionDTO dto) {
        LoginResponse.UserInfoVO approver = requireApprovalModuleAccess();
        BizApprovalOrder entity = claimPendingOrder(id, approver);
        BizDocumentMeta currentMeta = resolveBizMeta(entity.getBizType(), entity.getBizId());

        entity.setBeforeBizStatus(currentMeta.bizStatus());
        entity.setBeforeBizSnapshot(currentMeta.snapshot());

        finalizeReject(entity.getId(), dto, currentMeta);
    }

    private void executeVoidByApproval(BizApprovalOrder entity) {
        DocumentVoidDTO voidDTO = new DocumentVoidDTO();
        voidDTO.setReason(defaultReason(entity.getRequestReason()));
        voidDTO.setCreateRedFlush(ACTION_VOID_RED.equals(entity.getRequestAction()));

        switch (entity.getBizType()) {
            case "purchase" -> purchaseService.voidDocument(entity.getBizId(), voidDTO);
            case "purchase_return" -> purchaseReturnService.voidDocument(entity.getBizId(), voidDTO);
            case "sales" -> salesService.voidDocument(entity.getBizId(), voidDTO);
            case "sales_return" -> salesReturnService.voidDocument(entity.getBizId(), voidDTO);
            default -> throw BusinessException.validateFail("不支持的业务类型: " + entity.getBizType());
        }
    }

    private BizApprovalOrder claimPendingOrder(Long id, LoginResponse.UserInfoVO approver) {
        BizApprovalOrder entity = bizApprovalOrderMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("审批单不存在");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(entity.getStatus())) {
            throw BusinessException.validateFail("审批单已处理，不能重复操作");
        }

        LambdaUpdateWrapper<BizApprovalOrder> claimWrapper = new LambdaUpdateWrapper<>();
        claimWrapper.eq(BizApprovalOrder::getId, id)
                .eq(BizApprovalOrder::getStatus, STATUS_PENDING)
                .eq(BizApprovalOrder::getIsDeleted, 0)
                .set(BizApprovalOrder::getStatus, STATUS_PROCESSING)
                .set(BizApprovalOrder::getApproverId, approver.getId())
                .set(BizApprovalOrder::getApproverName, approver.getRealName());

        int rows = bizApprovalOrderMapper.update(null, claimWrapper);
        if (rows != 1) {
            throw BusinessException.validateFail("审批单已被其他管理员处理，请刷新后重试");
        }
        return entity;
    }

    private void finalizeApprove(Long id, ApprovalDecisionDTO dto, BizDocumentMeta beforeMeta, BizDocumentMeta afterMeta) {
        LambdaUpdateWrapper<BizApprovalOrder> approveWrapper = new LambdaUpdateWrapper<>();
        approveWrapper.eq(BizApprovalOrder::getId, id)
                .eq(BizApprovalOrder::getStatus, STATUS_PROCESSING)
                .eq(BizApprovalOrder::getIsDeleted, 0)
                .set(BizApprovalOrder::getStatus, STATUS_APPROVED)
                .set(BizApprovalOrder::getApproveRemark,
                    normalizeAndValidateLength("审批备注", dto == null ? null : dto.getRemark(), APPROVAL_TEXT_MAX_LEN))
                .set(BizApprovalOrder::getBeforeBizStatus, beforeMeta.bizStatus())
                .set(BizApprovalOrder::getBeforeBizSnapshot, beforeMeta.snapshot())
                .set(BizApprovalOrder::getAfterBizStatus, afterMeta.bizStatus())
                .set(BizApprovalOrder::getAfterBizSnapshot, afterMeta.snapshot())
                .set(BizApprovalOrder::getApprovedAt, LocalDateTime.now())
                .set(BizApprovalOrder::getRejectedAt, null);
        int rows = bizApprovalOrderMapper.update(null, approveWrapper);
        if (rows != 1) {
            throw BusinessException.validateFail("审批单状态已变化，无法完成审批");
        }
    }

    private void finalizeReject(Long id, ApprovalDecisionDTO dto, BizDocumentMeta currentMeta) {
        LambdaUpdateWrapper<BizApprovalOrder> rejectWrapper = new LambdaUpdateWrapper<>();
        rejectWrapper.eq(BizApprovalOrder::getId, id)
                .eq(BizApprovalOrder::getStatus, STATUS_PROCESSING)
                .eq(BizApprovalOrder::getIsDeleted, 0)
                .set(BizApprovalOrder::getStatus, STATUS_REJECTED)
                .set(BizApprovalOrder::getApproveRemark,
                    normalizeAndValidateLength("审批备注", dto == null ? null : dto.getRemark(), APPROVAL_TEXT_MAX_LEN))
                .set(BizApprovalOrder::getBeforeBizStatus, currentMeta.bizStatus())
                .set(BizApprovalOrder::getBeforeBizSnapshot, currentMeta.snapshot())
                .set(BizApprovalOrder::getAfterBizStatus, currentMeta.bizStatus())
                .set(BizApprovalOrder::getAfterBizSnapshot, currentMeta.snapshot())
                .set(BizApprovalOrder::getRejectedAt, LocalDateTime.now())
                .set(BizApprovalOrder::getApprovedAt, null);
        int rows = bizApprovalOrderMapper.update(null, rejectWrapper);
        if (rows != 1) {
            throw BusinessException.validateFail("审批单状态已变化，无法完成驳回");
        }
    }

    private void ensureNoPendingApproval(String bizType, Long bizId) {
        LambdaQueryWrapper<BizApprovalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizApprovalOrder::getBizType, bizType)
                .eq(BizApprovalOrder::getBizId, bizId)
                .eq(BizApprovalOrder::getStatus, STATUS_PENDING);
        if (bizApprovalOrderMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("该单据已存在待审批申请，请勿重复提交");
        }
    }

    private void ensureCanSubmitVoidApproval(BizDocumentMeta meta) {
        if (!Integer.valueOf(1).equals(meta.bizStatus())) {
            throw BusinessException.validateFail("仅正常状态单据可提交作废审批");
        }
        LocalDate bizDate = meta.bizTime() == null ? null : meta.bizTime().toLocalDate();
        if (bizDate != null && LocalDate.now().equals(bizDate)) {
            throw BusinessException.validateFail("当天单据请直接删除，无需提交作废审批");
        }
    }

    private BizDocumentMeta resolveBizMeta(String bizType, Long bizId) {
        switch (bizType) {
            case "purchase" -> {
                BizPurchase purchase = bizPurchaseMapper.selectById(bizId);
                if (purchase == null) {
                    throw BusinessException.notFound("进货单不存在");
                }
                LocalDateTime bizTime = purchase.getOperationTime() == null ? purchase.getCreateTime() : purchase.getOperationTime();
                return new BizDocumentMeta(purchase.getPurchaseNo(), purchase.getBizStatus(),
                        bizTime, buildSnapshot("purchase", purchase.getId(), purchase.getPurchaseNo(), purchase.getBizStatus(),
                        bizTime, purchase.getVoidTime(), purchase.getVoidReason(), purchase.getQuantity(), purchase.getTotalPrice(), purchase.getSourceId()));
            }
            case "purchase_return" -> {
                BizPurchaseReturn purchaseReturn = bizPurchaseReturnMapper.selectById(bizId);
                if (purchaseReturn == null) {
                    throw BusinessException.notFound("进货退货单不存在");
                }
                LocalDateTime bizTime = purchaseReturn.getOperationTime() == null ? purchaseReturn.getCreateTime() : purchaseReturn.getOperationTime();
                return new BizDocumentMeta(purchaseReturn.getReturnNo(), purchaseReturn.getBizStatus(),
                        bizTime, buildSnapshot("purchase_return", purchaseReturn.getId(), purchaseReturn.getReturnNo(), purchaseReturn.getBizStatus(),
                        bizTime, purchaseReturn.getVoidTime(), purchaseReturn.getVoidReason(), purchaseReturn.getQuantity(), purchaseReturn.getTotalPrice(), purchaseReturn.getSourceId()));
            }
            case "sales" -> {
                BizSales sales = bizSalesMapper.selectById(bizId);
                if (sales == null) {
                    throw BusinessException.notFound("销售单不存在");
                }
                LocalDateTime bizTime = sales.getOperationTime() == null ? sales.getCreateTime() : sales.getOperationTime();
                return new BizDocumentMeta(sales.getSalesNo(), sales.getBizStatus(),
                        bizTime, buildSnapshot("sales", sales.getId(), sales.getSalesNo(), sales.getBizStatus(),
                        bizTime, sales.getVoidTime(), sales.getVoidReason(), sales.getQuantity(), sales.getTotalPrice(), sales.getSourceId()));
            }
            case "sales_return" -> {
                BizSalesReturn salesReturn = bizSalesReturnMapper.selectById(bizId);
                if (salesReturn == null) {
                    throw BusinessException.notFound("销售退货单不存在");
                }
                LocalDateTime bizTime = salesReturn.getOperationTime() == null ? salesReturn.getCreateTime() : salesReturn.getOperationTime();
                return new BizDocumentMeta(salesReturn.getReturnNo(), salesReturn.getBizStatus(),
                        bizTime, buildSnapshot("sales_return", salesReturn.getId(), salesReturn.getReturnNo(), salesReturn.getBizStatus(),
                        bizTime, salesReturn.getVoidTime(), salesReturn.getVoidReason(), salesReturn.getQuantity(), salesReturn.getTotalPrice(), salesReturn.getSourceId()));
            }
            default -> throw BusinessException.validateFail("不支持的业务类型: " + bizType);
        }
    }

    private String buildSnapshot(String bizType,
                                 Long bizId,
                                 String bizNo,
                                 Integer bizStatus,
                                 LocalDateTime bizTime,
                                 LocalDateTime voidTime,
                                 String voidReason,
                                 Integer quantity,
                                 Object totalAmount,
                                 Long sourceId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("bizType", bizType);
        snapshot.put("bizId", bizId);
        snapshot.put("bizNo", bizNo);
        snapshot.put("bizStatus", bizStatus);
        snapshot.put("operationTime", bizTime);
        snapshot.put("voidTime", voidTime);
        snapshot.put("voidReason", voidReason);
        snapshot.put("quantity", quantity);
        snapshot.put("totalAmount", totalAmount);
        snapshot.put("sourceId", sourceId);
        return JSONUtil.toJsonStr(snapshot);
    }

    private void validateBizType(String bizType) {
        if ("purchase".equals(bizType)
                || "purchase_return".equals(bizType)
                || "sales".equals(bizType)
                || "sales_return".equals(bizType)) {
            return;
        }
        throw BusinessException.validateFail("不支持的业务类型: " + bizType);
    }

    private void validateAction(String action) {
        if (ACTION_VOID.equals(action) || ACTION_VOID_RED.equals(action)) {
            return;
        }
        throw BusinessException.validateFail("不支持的申请动作: " + action);
    }

    private void ensureRequesterCanSubmitApproval(LoginResponse.UserInfoVO requester, String role, String bizType) {
        if (ROLE_EMPLOYEE.equals(role)) {
            return;
        }
        if (!ROLE_ADMIN.equals(role)) {
            throw BusinessException.forbidden("当前角色无权提交作废审批申请");
        }

        String deptCode = authzService.normalizeDeptCode(requester.getDeptCode());
        boolean purchaseAdminRequest = AuthzService.DEPT_PURCHASE.equals(deptCode)
                && ("purchase".equals(bizType) || "purchase_return".equals(bizType));
        boolean salesAdminRequest = AuthzService.DEPT_SALES.equals(deptCode)
                && ("sales".equals(bizType) || "sales_return".equals(bizType));
        if (purchaseAdminRequest || salesAdminRequest) {
            return;
        }
        throw BusinessException.forbidden("仅采购或销售部门管理员可提交该类作废审批申请");
    }

    private LoginResponse.UserInfoVO requireApprovalModuleAccess() {
        LoginResponse.UserInfoVO user = requireLoginUser();
        authzService.requireDeptAdminOrSuperAdmin(AuthzService.DEPT_WAREHOUSE, "仅仓储部门管理员可执行审批操作");
        return user;
    }

    private LoginResponse.UserInfoVO requireLoginUser() {
        LoginResponse.UserInfoVO user = authService.getUserInfo();
        if (user == null || user.getId() == null) {
            throw BusinessException.unauthorized("用户未登录");
        }
        return user;
    }

    private ApprovalOrderVO toVO(BizApprovalOrder entity) {
        ApprovalOrderVO vo = new ApprovalOrderVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private String generateApprovalNo() {
        return "APR" + LocalDateTime.now().format(APPROVAL_NO_TIME_FMT)
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private String trimText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.trim();
    }

    private String normalizeAndValidateLength(String fieldName, String text, int maxLen) {
        String normalized = trimText(text);
        if (normalized.length() > maxLen) {
            throw BusinessException.validateFail(fieldName + "长度不能超过" + maxLen + "个字符");
        }
        return normalized;
    }

    private String defaultReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "手工作废";
        }
        return reason.trim();
    }

    private record BizDocumentMeta(String bizNo, Integer bizStatus, LocalDateTime bizTime, String snapshot) {
    }
}
