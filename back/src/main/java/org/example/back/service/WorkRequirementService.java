package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.WorkRequirementCreateDTO;
import org.example.back.dto.WorkRequirementExecuteDTO;
import org.example.back.dto.WorkRequirementQueryDTO;
import org.example.back.entity.SysUser;
import org.example.back.entity.WorkRequirement;
import org.example.back.entity.WorkRequirementAssign;
import org.example.back.entity.WorkRequirementAttachment;
import org.example.back.mapper.SysUserMapper;
import org.example.back.mapper.WorkRequirementAssignMapper;
import org.example.back.mapper.WorkRequirementAttachmentMapper;
import org.example.back.mapper.WorkRequirementMapper;
import org.example.back.vo.ReminderSummaryVO;
import org.example.back.vo.WorkRequirementAssignVO;
import org.example.back.vo.WorkRequirementDetailVO;
import org.example.back.vo.WorkRequirementTipVO;
import org.example.back.vo.WorkRequirementVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkRequirementService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_EXECUTING = 1;
    private static final int STATUS_PENDING_REVIEW = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_REJECTED = 4;
    private static final int STATUS_RETURNED = 5;
    private static final int OVERDUE_FLAG_NO = 0;
    private static final int OVERDUE_FLAG_YES = 1;
    private static final int SUBMITTED_ON_TIME_YES = 1;
    private static final int SUBMITTED_ON_TIME_NO = 0;

    @Autowired
    private WorkRequirementMapper workRequirementMapper;

    @Autowired
    private WorkRequirementAssignMapper assignMapper;

    @Autowired
    private WorkRequirementAttachmentMapper attachmentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AuthzService authzService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private WorkRequirementAttachmentStorageService attachmentStorageService;

    // ========== 管理员端 ==========

    public PageResult<WorkRequirementVO> page(WorkRequirementQueryDTO queryDTO) {
        authzService.requireAdminOrSuperAdmin("仅管理员可查看工作要求列表");
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();

        LambdaQueryWrapper<WorkRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkRequirement::getDeptId, currentUser.getDeptId())
                .like(StringUtils.hasText(queryDTO.getKeyword()), WorkRequirement::getContent, queryDTO.getKeyword())
                .orderByDesc(WorkRequirement::getCreateTime)
                .orderByDesc(WorkRequirement::getId);

        List<WorkRequirement> records = workRequirementMapper.selectList(wrapper);
        if (records == null || records.isEmpty()) {
            return PageResult.empty(queryDTO.getPageNum(), queryDTO.getPageSize());
        }

        List<Long> reqIds = records.stream().map(WorkRequirement::getId).toList();

        Map<Long, List<WorkRequirementAssign>> assignMap = Collections.emptyMap();
        if (!reqIds.isEmpty()) {
            LambdaQueryWrapper<WorkRequirementAssign> assignWrapper = new LambdaQueryWrapper<>();
            assignWrapper.in(WorkRequirementAssign::getRequirementId, reqIds);
            List<WorkRequirementAssign> allAssigns = assignMapper.selectList(assignWrapper);
            assignMap = allAssigns.stream().collect(Collectors.groupingBy(WorkRequirementAssign::getRequirementId));
        }

        Map<Long, List<WorkRequirementAssign>> finalAssignMap = assignMap;
        Integer filterStatus = queryDTO.getStatus();

        List<WorkRequirementVO> filteredList = records.stream()
                .map(req -> buildRequirementVO(req, finalAssignMap.getOrDefault(req.getId(), List.of())))
                .filter(vo -> matchesSummaryStatus(vo, filterStatus))
                .filter(vo -> matchesOverdueType(vo, queryDTO.getOverdueType()))
                .toList();

        long current = queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1 ? 1L : queryDTO.getPageNum();
        long size = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10L : queryDTO.getPageSize();
        long total = filteredList.size();
        long pages = total == 0 ? 0L : (total + size - 1) / size;
        int fromIndex = (int) Math.min((current - 1) * size, total);
        int toIndex = (int) Math.min(fromIndex + size, total);
        List<WorkRequirementVO> pageRecords = filteredList.subList(fromIndex, toIndex);

        return new PageResult<>(pageRecords, total, current, size, pages);
    }

    public ReminderSummaryVO pendingReviewReminder() {
        authzService.requireAdminOrSuperAdmin("仅管理员可查看工作要求待审核提醒");
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();

        ReminderSummaryVO reminder = new ReminderSummaryVO();
        reminder.setCount(0L);
        reminder.setSignature("");

        if (currentUser.getDeptId() == null) {
            return reminder;
        }

        LambdaQueryWrapper<WorkRequirement> reqWrapper = new LambdaQueryWrapper<>();
        reqWrapper.eq(WorkRequirement::getDeptId, currentUser.getDeptId())
                .select(WorkRequirement::getId);
        List<WorkRequirement> requirements = workRequirementMapper.selectList(reqWrapper);
        if (requirements == null || requirements.isEmpty()) {
            return reminder;
        }

        List<Long> requirementIds = requirements.stream()
                .map(WorkRequirement::getId)
                .filter(id -> id != null)
                .toList();
        if (requirementIds.isEmpty()) {
            return reminder;
        }

        LambdaQueryWrapper<WorkRequirementAssign> assignWrapper = new LambdaQueryWrapper<>();
        assignWrapper.in(WorkRequirementAssign::getRequirementId, requirementIds)
                .eq(WorkRequirementAssign::getStatus, STATUS_PENDING_REVIEW)
                .orderByAsc(WorkRequirementAssign::getId);
        List<WorkRequirementAssign> pendingAssigns = assignMapper.selectList(assignWrapper);
        if (pendingAssigns == null || pendingAssigns.isEmpty()) {
            return reminder;
        }

        reminder.setCount((long) pendingAssigns.size());
        reminder.setSignature(pendingAssigns.stream()
                .map(WorkRequirementAssign::getId)
                .filter(id -> id != null)
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        return reminder;
    }

    public ReminderSummaryVO overdueReminder() {
        authzService.requireAdminOrSuperAdmin("仅管理员可查看工作要求超时提醒");
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();

        ReminderSummaryVO reminder = new ReminderSummaryVO();
        reminder.setCount(0L);
        reminder.setSignature("");

        if (currentUser.getDeptId() == null) {
            return reminder;
        }

        LambdaQueryWrapper<WorkRequirement> reqWrapper = new LambdaQueryWrapper<>();
        reqWrapper.eq(WorkRequirement::getDeptId, currentUser.getDeptId());
        List<WorkRequirement> requirements = workRequirementMapper.selectList(reqWrapper);
        if (requirements == null || requirements.isEmpty()) {
            return reminder;
        }

        Map<Long, WorkRequirement> requirementMap = requirements.stream()
                .filter(req -> req.getId() != null)
                .collect(Collectors.toMap(WorkRequirement::getId, req -> req));
        if (requirementMap.isEmpty()) {
            return reminder;
        }

        LambdaQueryWrapper<WorkRequirementAssign> assignWrapper = new LambdaQueryWrapper<>();
        assignWrapper.in(WorkRequirementAssign::getRequirementId, requirementMap.keySet())
                .in(WorkRequirementAssign::getStatus, List.of(STATUS_PENDING, STATUS_EXECUTING, STATUS_RETURNED))
                .orderByAsc(WorkRequirementAssign::getId);
        List<WorkRequirementAssign> assigns = assignMapper.selectList(assignWrapper);
        if (assigns == null || assigns.isEmpty()) {
            return reminder;
        }

        List<WorkRequirementAssign> overdueAssigns = assigns.stream()
                .filter(assign -> isOverdueCurrent(assign, requirementMap.get(assign.getRequirementId())))
                .toList();
        if (overdueAssigns.isEmpty()) {
            return reminder;
        }

        reminder.setCount((long) overdueAssigns.size());
        reminder.setSignature(overdueAssigns.stream()
                .map(WorkRequirementAssign::getId)
                .filter(id -> id != null)
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        return reminder;
    }

    public WorkRequirementDetailVO getDetail(Long id) {
        authzService.requireAdminOrSuperAdmin("仅管理员可查看工作要求详情");
        WorkRequirement req = requireRequirement(id);
        authzService.requireCurrentDept(req.getDeptId(), "无权查看其他部门的工作要求");

        WorkRequirementDetailVO vo = new WorkRequirementDetailVO();
        vo.setId(req.getId());
        vo.setContent(req.getContent());
        vo.setStartTime(req.getStartTime());
        vo.setEndTime(req.getEndTime());
        vo.setTargetScope(req.getTargetScope());
        vo.setCreatorName(req.getCreatorName());
        vo.setCreateTime(req.getCreateTime());

        LambdaQueryWrapper<WorkRequirementAssign> assignWrapper = new LambdaQueryWrapper<>();
        assignWrapper.eq(WorkRequirementAssign::getRequirementId, id).orderByAsc(WorkRequirementAssign::getId);
        List<WorkRequirementAssign> assigns = assignMapper.selectList(assignWrapper);

        List<Long> assignIds = assigns.stream().map(WorkRequirementAssign::getId).toList();
        Map<Long, List<WorkRequirementAttachment>> attachMap = Collections.emptyMap();
        if (!assignIds.isEmpty()) {
            LambdaQueryWrapper<WorkRequirementAttachment> attWrapper = new LambdaQueryWrapper<>();
            attWrapper.in(WorkRequirementAttachment::getAssignId, assignIds);
            attachMap = attachmentMapper.selectList(attWrapper).stream()
                    .collect(Collectors.groupingBy(WorkRequirementAttachment::getAssignId));
        }

        Map<Long, List<WorkRequirementAttachment>> finalAttachMap = attachMap;
        vo.setAssigns(assigns.stream().map(a -> {
            WorkRequirementDetailVO.AssignItemVO item = new WorkRequirementDetailVO.AssignItemVO();
            item.setAssignId(a.getId());
            item.setEmployeeUserId(a.getEmployeeUserId());
            item.setEmployeeName(a.getEmployeeName());
            item.setStatus(a.getStatus());
            item.setStatusLabel(statusLabel(a.getStatus()));
            item.setOverdueFlag(resolveOverdueFlag(a, req));
            item.setOverdueAt(resolveOverdueAt(a, req));
            item.setSubmittedOnTime(resolveSubmittedOnTime(a, req));
            item.setCompletedAt(resolveCompletedAt(a));
            item.setOverdueCurrent(isOverdueCurrent(a, req));
            item.setLateSubmission(isLateSubmission(a, req));
            item.setOverdueLabel(overdueLabel(a, req));
            item.setExecuteResult(a.getExecuteResult());
            item.setRejectCount(a.getRejectCount());
            item.setAcceptedAt(a.getAcceptedAt());
            item.setSubmittedAt(a.getSubmittedAt());
            item.setReviewedAt(a.getReviewedAt());
            item.setReviewerName(a.getReviewerName());
            item.setAttachments(finalAttachMap.getOrDefault(a.getId(), List.of()).stream().map(att -> {
                WorkRequirementDetailVO.AttachmentVO attVO = new WorkRequirementDetailVO.AttachmentVO();
                attVO.setId(att.getId());
                attVO.setFileName(att.getFileName());
                attVO.setFilePath(att.getFilePath());
                attVO.setFileSize(att.getFileSize());
                return attVO;
            }).toList());
            return item;
        }).toList());

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(WorkRequirementCreateDTO dto) {
        authzService.requireAdminOrSuperAdmin("仅管理员可创建工作要求");
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();

        if (currentUser.getDeptId() == null) {
            throw BusinessException.validateFail("当前管理员未配置所属部门，无法创建工作要求");
        }
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw BusinessException.validateFail("截止时间不能早于开始时间");
        }

        WorkRequirement req = new WorkRequirement();
        req.setContent(dto.getContent());
        req.setStartTime(dto.getStartTime());
        req.setEndTime(dto.getEndTime());
        req.setTargetScope(dto.getTargetScope());
        req.setCreatorId(currentUser.getId());
        req.setCreatorName(currentUser.getRealName());
        req.setDeptId(currentUser.getDeptId());
        req.setDeptCode(currentUser.getDeptCode());
        workRequirementMapper.insert(req);

        // 确定分配的员工列表
        List<SysUser> employees;
        if ("selected".equals(dto.getTargetScope())) {
            if (dto.getEmployeeUserIds() == null || dto.getEmployeeUserIds().isEmpty()) {
                throw BusinessException.validateFail("指定员工模式下必须选择至少一名员工");
            }
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.in(SysUser::getId, dto.getEmployeeUserIds())
                    .eq(SysUser::getDeptId, currentUser.getDeptId())
                    .eq(SysUser::getRole, "employee")
                    .eq(SysUser::getStatus, 1);
            employees = sysUserMapper.selectList(userWrapper);
            if (employees.isEmpty()) {
                throw BusinessException.validateFail("所选员工不存在或不属于当前部门");
            }
        } else {
            // all - 全体部门员工
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(SysUser::getDeptId, currentUser.getDeptId())
                    .eq(SysUser::getRole, "employee")
                    .eq(SysUser::getStatus, 1);
            employees = sysUserMapper.selectList(userWrapper);
            if (employees.isEmpty()) {
                throw BusinessException.validateFail("当前部门没有启用的员工");
            }
        }

        // 批量创建分配记录
        for (SysUser emp : employees) {
            WorkRequirementAssign assign = new WorkRequirementAssign();
            assign.setRequirementId(req.getId());
            assign.setEmployeeUserId(emp.getId());
            assign.setEmployeeName(emp.getRealName());
            assign.setStatus(STATUS_PENDING);
            assign.setOverdueFlag(OVERDUE_FLAG_NO);
            assign.setOverdueRemindCount(0);
            assign.setRejectCount(0);
            assignMapper.insert(assign);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        authzService.requireAdminOrSuperAdmin("仅管理员可删除工作要求");
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        WorkRequirement req = requireRequirement(id);

        if (!req.getCreatorId().equals(currentUser.getId())) {
            throw BusinessException.forbidden("只能删除自己创建的工作要求");
        }

        LambdaQueryWrapper<WorkRequirementAssign> assignWrapper = new LambdaQueryWrapper<>();
        assignWrapper.eq(WorkRequirementAssign::getRequirementId, id);
        List<WorkRequirementAssign> assigns = assignMapper.selectList(assignWrapper);
        cleanupAttachmentsByAssignIds(assigns.stream().map(WorkRequirementAssign::getId).toList());

        workRequirementMapper.deleteById(id);
        for (WorkRequirementAssign assign : assigns) {
            assignMapper.deleteById(assign.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Long assignId, boolean approved) {
        authzService.requireAdminOrSuperAdmin("仅管理员可审核工作要求");
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();

        WorkRequirementAssign assign = requireAssign(assignId);
        WorkRequirement req = requireRequirement(assign.getRequirementId());
        authzService.requireCurrentDept(req.getDeptId(), "无权审核其他部门的工作要求");

        if (assign.getStatus() != STATUS_PENDING_REVIEW) {
            throw BusinessException.validateFail("当前状态不可审核，仅待审核状态可操作");
        }

        assign.setReviewedAt(LocalDateTime.now());
        assign.setReviewerId(currentUser.getId());
        assign.setReviewerName(currentUser.getRealName());

        if (approved) {
            assign.setStatus(STATUS_COMPLETED);
            assign.setCompletedAt(assign.getReviewedAt());
            syncSubmitTimeliness(assign, req);
        } else {
            assign.setStatus(STATUS_RETURNED);
            assign.setRejectCount(assign.getRejectCount() + 1);
            // 清除旧的执行结果和附件，以便重新填写
            assign.setExecuteResult(null);
            assign.setSubmittedAt(null);
            assign.setSubmittedOnTime(null);
            assign.setCompletedAt(null);
            cleanupAttachmentsByAssignIds(List.of(assignId));
        }

        assignMapper.updateById(assign);
    }

    /**
     * 获取当前管理员部门下的员工列表（用于选人下拉）
     */
    public List<DeptEmployeeOption> getDeptEmployees() {
        authzService.requireAdminOrSuperAdmin("仅管理员可获取部门员工列表");
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeptId, currentUser.getDeptId())
                .eq(SysUser::getRole, "employee")
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getRealName);
        List<SysUser> users = sysUserMapper.selectList(wrapper);

        return users.stream().map(u -> {
            DeptEmployeeOption option = new DeptEmployeeOption();
            option.setUserId(u.getId());
            option.setRealName(u.getRealName());
            option.setUsername(u.getUsername());
            return option;
        }).toList();
    }

    // ========== 员工端 ==========

    public List<WorkRequirementAssignVO> getMyRequirements() {
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();

        LambdaQueryWrapper<WorkRequirementAssign> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkRequirementAssign::getEmployeeUserId, currentUser.getId())
                .orderByDesc(WorkRequirementAssign::getCreateTime);
        List<WorkRequirementAssign> assigns = assignMapper.selectList(wrapper);

        if (assigns.isEmpty()) {
            return List.of();
        }

        List<Long> reqIds = assigns.stream().map(WorkRequirementAssign::getRequirementId).distinct().toList();
        LambdaQueryWrapper<WorkRequirement> reqWrapper = new LambdaQueryWrapper<>();
        reqWrapper.in(WorkRequirement::getId, reqIds);
        Map<Long, WorkRequirement> reqMap = workRequirementMapper.selectList(reqWrapper).stream()
                .collect(Collectors.toMap(WorkRequirement::getId, r -> r));

        return assigns.stream().map(a -> toAssignVO(a, reqMap.get(a.getRequirementId()))).toList();
    }

    public WorkRequirementAssignVO getAssignDetail(Long assignId) {
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        WorkRequirementAssign assign = requireAssign(assignId);

        if (!assign.getEmployeeUserId().equals(currentUser.getId())) {
            throw BusinessException.forbidden("无权查看他人的工作要求详情");
        }

        WorkRequirement req = requireRequirement(assign.getRequirementId());

        WorkRequirementAssignVO vo = toAssignVO(assign, req);

        // 加载附件
        LambdaQueryWrapper<WorkRequirementAttachment> attWrapper = new LambdaQueryWrapper<>();
        attWrapper.eq(WorkRequirementAttachment::getAssignId, assignId);
        List<WorkRequirementAttachment> atts = attachmentMapper.selectList(attWrapper);
        vo.setAttachments(atts.stream().map(att -> {
            WorkRequirementAssignVO.AttachmentVO attVO = new WorkRequirementAssignVO.AttachmentVO();
            attVO.setId(att.getId());
            attVO.setFileName(att.getFileName());
            attVO.setFilePath(att.getFilePath());
            attVO.setFileSize(att.getFileSize());
            return attVO;
        }).toList());

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void accept(Long assignId) {
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        WorkRequirementAssign assign = requireAssign(assignId);
        WorkRequirement req = requireRequirement(assign.getRequirementId());

        if (!assign.getEmployeeUserId().equals(currentUser.getId())) {
            throw BusinessException.forbidden("无权操作他人的工作要求");
        }
        if (assign.getStatus() != STATUS_PENDING) {
            throw BusinessException.validateFail("当前状态不可接受，仅待接受状态可操作");
        }

        assign.setStatus(STATUS_EXECUTING);
        assign.setAcceptedAt(LocalDateTime.now());
        if (req.getEndTime() != null && LocalDateTime.now().isAfter(req.getEndTime())) {
            assign.setOverdueFlag(OVERDUE_FLAG_YES);
            if (assign.getOverdueAt() == null) {
                assign.setOverdueAt(req.getEndTime());
            }
        }
        assignMapper.updateById(assign);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long assignId) {
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        WorkRequirementAssign assign = requireAssign(assignId);

        if (!assign.getEmployeeUserId().equals(currentUser.getId())) {
            throw BusinessException.forbidden("无权操作他人的工作要求");
        }
        if (assign.getStatus() != STATUS_PENDING) {
            throw BusinessException.validateFail("当前状态不可拒绝，仅待接受状态可操作");
        }

        assign.setStatus(STATUS_REJECTED);
        assignMapper.updateById(assign);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitExecution(WorkRequirementExecuteDTO dto) {
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        WorkRequirementAssign assign = requireAssign(dto.getAssignId());
        WorkRequirement req = requireRequirement(assign.getRequirementId());

        if (!assign.getEmployeeUserId().equals(currentUser.getId())) {
            throw BusinessException.forbidden("无权操作他人的工作要求");
        }
        if (assign.getStatus() != STATUS_EXECUTING && assign.getStatus() != STATUS_RETURNED) {
            throw BusinessException.validateFail("当前状态不可提交，仅执行中或已驳回状态可操作");
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        assign.setStatus(STATUS_PENDING_REVIEW);
        assign.setExecuteResult(dto.getExecuteResult());
        assign.setSubmittedAt(submittedAt);
        if (req.getEndTime() != null && submittedAt.isAfter(req.getEndTime())) {
            assign.setSubmittedOnTime(SUBMITTED_ON_TIME_NO);
            assign.setOverdueFlag(OVERDUE_FLAG_YES);
            if (assign.getOverdueAt() == null) {
                assign.setOverdueAt(req.getEndTime());
            }
        } else {
            assign.setSubmittedOnTime(SUBMITTED_ON_TIME_YES);
        }
        assign.setCompletedAt(null);
        assignMapper.updateById(assign);

        LambdaQueryWrapper<WorkRequirementAttachment> oldAttWrapper = new LambdaQueryWrapper<>();
        oldAttWrapper.eq(WorkRequirementAttachment::getAssignId, dto.getAssignId());
        List<WorkRequirementAttachment> currentAttachments = attachmentMapper.selectList(oldAttWrapper);
        Map<Long, WorkRequirementAttachment> currentAttachmentMap = currentAttachments.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(WorkRequirementAttachment::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Long> existingAttachmentIds = sanitizeExistingAttachmentIds(dto.getExistingAttachmentIds());
        List<WorkRequirementAttachment> attachmentsToKeep = new ArrayList<>();
        for (Long attachmentId : existingAttachmentIds) {
            WorkRequirementAttachment existingAttachment = currentAttachmentMap.get(attachmentId);
            if (existingAttachment == null) {
                throw BusinessException.forbidden("附件引用无效或不属于当前工作要求");
            }
            attachmentsToKeep.add(existingAttachment);
        }

        List<WorkRequirementAttachmentStorageService.TempUploadMeta> newAttachments = resolveNewAttachments(dto.getNewAttachmentTokens());

        // 删除旧附件（驳回后重新提交的场景）
        cleanupAttachmentEntities(currentAttachments);

        for (WorkRequirementAttachment existingAttachment : attachmentsToKeep) {
            WorkRequirementAttachment clonedAttachment = new WorkRequirementAttachment();
            clonedAttachment.setAssignId(dto.getAssignId());
            clonedAttachment.setFileName(existingAttachment.getFileName());
            clonedAttachment.setFilePath(existingAttachment.getFilePath());
            clonedAttachment.setFileSize(existingAttachment.getFileSize());
            attachmentMapper.insert(clonedAttachment);
        }

        for (WorkRequirementAttachmentStorageService.TempUploadMeta tempUpload : newAttachments) {
            WorkRequirementAttachment newAttachment = new WorkRequirementAttachment();
            newAttachment.setAssignId(dto.getAssignId());
            newAttachment.setFileName(tempUpload.getFileName());
            newAttachment.setFilePath(tempUpload.getStoredPath());
            newAttachment.setFileSize(tempUpload.getFileSize());
            attachmentMapper.insert(newAttachment);
        }
    }

    public AttachmentDownload getAccessibleAttachment(Long attachmentId) {
        WorkRequirementAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw BusinessException.notFound("附件不存在");
        }
        WorkRequirementAssign assign = requireAssign(attachment.getAssignId());
        WorkRequirement requirement = requireRequirement(assign.getRequirementId());

        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        if (authzService.isSuperAdmin()) {
            return new AttachmentDownload(attachment.getFileName(), attachment.getFilePath());
        }
        if (authzService.isAdmin()) {
            authzService.requireCurrentDept(requirement.getDeptId(), "无权访问其他部门的工作要求附件");
            return new AttachmentDownload(attachment.getFileName(), attachment.getFilePath());
        }
        if (!Objects.equals(assign.getEmployeeUserId(), currentUser.getId())) {
            throw BusinessException.forbidden("无权访问该附件");
        }
        return new AttachmentDownload(attachment.getFileName(), attachment.getFilePath());
    }

    @Transactional(rollbackFor = Exception.class)
    public int scanAndMarkOverdueAssignments() {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<WorkRequirement> reqWrapper = new LambdaQueryWrapper<>();
        reqWrapper.lt(WorkRequirement::getEndTime, now);
        List<WorkRequirement> requirements = workRequirementMapper.selectList(reqWrapper);
        if (requirements == null || requirements.isEmpty()) {
            return 0;
        }

        Map<Long, WorkRequirement> requirementMap = requirements.stream()
                .filter(req -> req.getId() != null)
                .collect(Collectors.toMap(WorkRequirement::getId, req -> req));
        if (requirementMap.isEmpty()) {
            return 0;
        }

        LambdaQueryWrapper<WorkRequirementAssign> assignWrapper = new LambdaQueryWrapper<>();
        assignWrapper.in(WorkRequirementAssign::getRequirementId, requirementMap.keySet())
                .in(WorkRequirementAssign::getStatus, List.of(STATUS_PENDING, STATUS_EXECUTING, STATUS_RETURNED))
                .and(wrapper -> wrapper.isNull(WorkRequirementAssign::getOverdueFlag)
                        .or()
                        .eq(WorkRequirementAssign::getOverdueFlag, OVERDUE_FLAG_NO))
                .orderByAsc(WorkRequirementAssign::getId);
        List<WorkRequirementAssign> candidates = assignMapper.selectList(assignWrapper);
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        int updated = 0;
        for (WorkRequirementAssign assign : candidates) {
            WorkRequirement req = requirementMap.get(assign.getRequirementId());
            if (req == null || req.getEndTime() == null || !now.isAfter(req.getEndTime())) {
                continue;
            }
            markAssignOverdue(assign, req.getEndTime(), now);
            assignMapper.updateById(assign);
            messageService.sendWorkRequirementOverdueToEmployee(assign.getEmployeeUserId(), req.getContent(), req.getEndTime());
            messageService.sendWorkRequirementOverdueToDeptAdmins(req.getDeptId(), assign.getEmployeeName(), req.getContent(), req.getEndTime());
            updated++;
        }
        return updated;
    }

    /**
     * 获取当前员工的工作提示数据（用于首页）
     */
    public List<WorkRequirementTipVO> getEmployeeWorkTips(Long userId) {
        LambdaQueryWrapper<WorkRequirementAssign> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkRequirementAssign::getEmployeeUserId, userId)
                .orderByAsc(WorkRequirementAssign::getStatus)
                .orderByDesc(WorkRequirementAssign::getCreateTime);
        List<WorkRequirementAssign> assigns = assignMapper.selectList(wrapper);

        if (assigns.isEmpty()) {
            return List.of();
        }

        List<Long> reqIds = assigns.stream().map(WorkRequirementAssign::getRequirementId).distinct().toList();
        LambdaQueryWrapper<WorkRequirement> reqWrapper = new LambdaQueryWrapper<>();
        reqWrapper.in(WorkRequirement::getId, reqIds);
        Map<Long, WorkRequirement> reqMap = workRequirementMapper.selectList(reqWrapper).stream()
                .collect(Collectors.toMap(WorkRequirement::getId, r -> r));

        List<WorkRequirementTipVO> result = new ArrayList<>();
        List<WorkRequirementTipVO> completedTips = new ArrayList<>();

        for (WorkRequirementAssign assign : assigns) {
            WorkRequirement req = reqMap.get(assign.getRequirementId());
            if (req == null) continue;

            WorkRequirementTipVO tip = new WorkRequirementTipVO();
            tip.setAssignId(assign.getId());
            tip.setContent(req.getContent());
            tip.setStatus(assign.getStatus());
            tip.setStatusLabel(statusLabel(assign.getStatus()));
            tip.setOverdueFlag(resolveOverdueFlag(assign, req));
            tip.setOverdueCurrent(isOverdueCurrent(assign, req));
            tip.setLateSubmission(isLateSubmission(assign, req));
            tip.setOverdueLabel(overdueLabel(assign, req));
            tip.setEndTime(req.getEndTime());

            if (assign.getStatus() == STATUS_COMPLETED || assign.getStatus() == STATUS_REJECTED) {
                completedTips.add(tip);
            } else {
                result.add(tip);
            }
        }

        // 已完成和拒收的排到最后
        result.addAll(completedTips);
        return result;
    }

    // ========== 辅助方法 ==========

    private WorkRequirement requireRequirement(Long id) {
        WorkRequirement req = workRequirementMapper.selectById(id);
        if (req == null) {
            throw BusinessException.notFound("工作要求不存在");
        }
        return req;
    }

    private WorkRequirementAssign requireAssign(Long assignId) {
        WorkRequirementAssign assign = assignMapper.selectById(assignId);
        if (assign == null) {
            throw BusinessException.notFound("工作要求分配记录不存在");
        }
        return assign;
    }

    private WorkRequirementAssignVO toAssignVO(WorkRequirementAssign assign, WorkRequirement req) {
        WorkRequirementAssignVO vo = new WorkRequirementAssignVO();
        vo.setAssignId(assign.getId());
        vo.setRequirementId(assign.getRequirementId());
        vo.setContent(req != null ? req.getContent() : null);
        vo.setStartTime(req != null ? req.getStartTime() : null);
        vo.setEndTime(req != null ? req.getEndTime() : null);
        vo.setStatus(assign.getStatus());
        vo.setStatusLabel(statusLabel(assign.getStatus()));
        vo.setOverdueFlag(resolveOverdueFlag(assign, req));
        vo.setOverdueAt(resolveOverdueAt(assign, req));
        vo.setSubmittedOnTime(resolveSubmittedOnTime(assign, req));
        vo.setCompletedAt(resolveCompletedAt(assign));
        vo.setOverdueCurrent(isOverdueCurrent(assign, req));
        vo.setLateSubmission(isLateSubmission(assign, req));
        vo.setOverdueLabel(overdueLabel(assign, req));
        vo.setExecuteResult(assign.getExecuteResult());
        vo.setRejectCount(assign.getRejectCount());
        vo.setAcceptedAt(assign.getAcceptedAt());
        vo.setSubmittedAt(assign.getSubmittedAt());
        vo.setReviewedAt(assign.getReviewedAt());
        vo.setReviewerName(assign.getReviewerName());
        vo.setAttachments(List.of());
        return vo;
    }

    private WorkRequirementVO buildRequirementVO(WorkRequirement req, List<WorkRequirementAssign> assigns) {
        WorkRequirementVO vo = new WorkRequirementVO();
        vo.setId(req.getId());
        vo.setContent(req.getContent());
        vo.setStartTime(req.getStartTime());
        vo.setEndTime(req.getEndTime());
        vo.setTargetScope(req.getTargetScope());
        vo.setCreatorName(req.getCreatorName());
        vo.setCreateTime(req.getCreateTime());
        vo.setTotalCount(assigns.size());
        vo.setCompletedCount((int) assigns.stream().filter(a -> a.getStatus() == STATUS_COMPLETED).count());
        vo.setPendingReviewCount((int) assigns.stream().filter(a -> a.getStatus() == STATUS_PENDING_REVIEW).count());
        vo.setRejectedCount((int) assigns.stream().filter(a -> a.getStatus() == STATUS_REJECTED).count());
        vo.setOverdueCount((int) assigns.stream().filter(a -> isOverdueCurrent(a, req)).count());
        vo.setOverdueSubmitCount((int) assigns.stream().filter(a -> isLateSubmission(a, req)).count());
        vo.setSummaryStatus(computeSummaryStatus(assigns));
        return vo;
    }

    private boolean matchesSummaryStatus(WorkRequirementVO vo, Integer filterStatus) {
        if (filterStatus == null) {
            return true;
        }
        String targetSummary = switch (filterStatus) {
            case 0 -> "未完成";
            case 2 -> "待审核";
            case 3 -> "已完成";
            case 4 -> "拒收";
            default -> null;
        };
        return targetSummary == null || targetSummary.equals(vo.getSummaryStatus());
    }

    private boolean matchesOverdueType(WorkRequirementVO vo, String overdueType) {
        if (!StringUtils.hasText(overdueType)) {
            return true;
        }
        return switch (overdueType.trim().toLowerCase()) {
            case "overdue" -> safeInt(vo.getOverdueCount()) > 0;
            case "overdue_submit" -> safeInt(vo.getOverdueSubmitCount()) > 0;
            case "normal" -> safeInt(vo.getOverdueCount()) == 0 && safeInt(vo.getOverdueSubmitCount()) == 0;
            default -> true;
        };
    }

    private String computeSummaryStatus(List<WorkRequirementAssign> assigns) {
        if (assigns.isEmpty()) return "未完成";

        boolean hasPendingReview = assigns.stream().anyMatch(a -> a.getStatus() == STATUS_PENDING_REVIEW);
        if (hasPendingReview) return "待审核";

        boolean allDone = assigns.stream().allMatch(a ->
                a.getStatus() == STATUS_COMPLETED || a.getStatus() == STATUS_REJECTED);
        if (allDone) {
            boolean allRejected = assigns.stream().allMatch(a -> a.getStatus() == STATUS_REJECTED);
            return allRejected ? "拒收" : "已完成";
        }

        return "未完成";
    }

    private void markAssignOverdue(WorkRequirementAssign assign, LocalDateTime overdueAt, LocalDateTime remindTime) {
        assign.setOverdueFlag(OVERDUE_FLAG_YES);
        if (assign.getOverdueAt() == null) {
            assign.setOverdueAt(overdueAt != null ? overdueAt : remindTime);
        }
        assign.setOverdueRemindCount(safeInt(assign.getOverdueRemindCount()) + 1);
        assign.setLastRemindTime(remindTime);
    }

    private void syncSubmitTimeliness(WorkRequirementAssign assign, WorkRequirement req) {
        if (assign.getSubmittedAt() == null || req == null || req.getEndTime() == null) {
            return;
        }
        if (assign.getSubmittedAt().isAfter(req.getEndTime())) {
            assign.setSubmittedOnTime(SUBMITTED_ON_TIME_NO);
            assign.setOverdueFlag(OVERDUE_FLAG_YES);
            if (assign.getOverdueAt() == null) {
                assign.setOverdueAt(req.getEndTime());
            }
            return;
        }
        if (assign.getSubmittedOnTime() == null) {
            assign.setSubmittedOnTime(SUBMITTED_ON_TIME_YES);
        }
    }

    private Integer resolveOverdueFlag(WorkRequirementAssign assign, WorkRequirement req) {
        if (assign.getOverdueFlag() != null) {
            return assign.getOverdueFlag();
        }
        if (isLateSubmission(assign, req)) {
            return OVERDUE_FLAG_YES;
        }
        if (req != null && req.getEndTime() != null && LocalDateTime.now().isAfter(req.getEndTime()) && isOverdueTrackableStatus(assign.getStatus())) {
            return OVERDUE_FLAG_YES;
        }
        return OVERDUE_FLAG_NO;
    }

    private LocalDateTime resolveOverdueAt(WorkRequirementAssign assign, WorkRequirement req) {
        if (assign.getOverdueAt() != null) {
            return assign.getOverdueAt();
        }
        return resolveOverdueFlag(assign, req) == OVERDUE_FLAG_YES && req != null ? req.getEndTime() : null;
    }

    private Integer resolveSubmittedOnTime(WorkRequirementAssign assign, WorkRequirement req) {
        if (assign.getSubmittedOnTime() != null) {
            return assign.getSubmittedOnTime();
        }
        if (assign.getSubmittedAt() == null || req == null || req.getEndTime() == null) {
            return null;
        }
        return assign.getSubmittedAt().isAfter(req.getEndTime()) ? SUBMITTED_ON_TIME_NO : SUBMITTED_ON_TIME_YES;
    }

    private LocalDateTime resolveCompletedAt(WorkRequirementAssign assign) {
        if (assign.getCompletedAt() != null) {
            return assign.getCompletedAt();
        }
        return assign.getStatus() == STATUS_COMPLETED ? assign.getReviewedAt() : null;
    }

    private boolean isOverdueCurrent(WorkRequirementAssign assign, WorkRequirement req) {
        return isOverdueTrackableStatus(assign.getStatus()) && resolveOverdueFlag(assign, req) == OVERDUE_FLAG_YES;
    }

    private boolean isLateSubmission(WorkRequirementAssign assign, WorkRequirement req) {
        Integer submittedOnTime = resolveSubmittedOnTime(assign, req);
        return submittedOnTime != null && submittedOnTime == SUBMITTED_ON_TIME_NO;
    }

    private boolean isOverdueTrackableStatus(Integer status) {
        return status != null && (status == STATUS_PENDING || status == STATUS_EXECUTING || status == STATUS_RETURNED);
    }

    private String overdueLabel(WorkRequirementAssign assign, WorkRequirement req) {
        if (isOverdueCurrent(assign, req)) {
            return "超时中";
        }
        if (isLateSubmission(assign, req)) {
            return assign.getStatus() != null && assign.getStatus() == STATUS_COMPLETED ? "逾期完成" : "逾期提交";
        }
        return "正常";
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void cleanupAttachmentsByAssignIds(List<Long> assignIds) {
        if (assignIds == null || assignIds.isEmpty()) {
            return;
        }
        List<Long> validAssignIds = assignIds.stream().filter(Objects::nonNull).distinct().toList();
        if (validAssignIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<WorkRequirementAttachment> attachmentWrapper = new LambdaQueryWrapper<>();
        attachmentWrapper.in(WorkRequirementAttachment::getAssignId, validAssignIds);
        List<WorkRequirementAttachment> attachments = attachmentMapper.selectList(attachmentWrapper);
        cleanupAttachmentEntities(attachments);
    }

    private void cleanupAttachmentEntities(List<WorkRequirementAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        List<Long> attachmentIds = attachments.stream()
                .map(WorkRequirementAttachment::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!attachmentIds.isEmpty()) {
            LambdaQueryWrapper<WorkRequirementAttachment> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.in(WorkRequirementAttachment::getId, attachmentIds);
            attachmentMapper.delete(deleteWrapper);
        }
        attachments.stream()
                .map(WorkRequirementAttachment::getFilePath)
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(attachmentStorageService::deleteStoredFileQuietly);
    }

    private List<Long> sanitizeExistingAttachmentIds(List<Long> existingAttachmentIds) {
        if (existingAttachmentIds == null || existingAttachmentIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniqueIds = existingAttachmentIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(uniqueIds);
    }

    private List<WorkRequirementAttachmentStorageService.TempUploadMeta> resolveNewAttachments(List<String> newAttachmentTokens) {
        if (newAttachmentTokens == null || newAttachmentTokens.isEmpty()) {
            return List.of();
        }
        Set<String> uniqueTokens = newAttachmentTokens.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<WorkRequirementAttachmentStorageService.TempUploadMeta> attachments = new ArrayList<>(uniqueTokens.size());
        for (String token : uniqueTokens) {
            attachments.add(attachmentStorageService.consumeTempUpload(token));
        }
        return attachments;
    }

    private String statusLabel(int status) {
        return switch (status) {
            case STATUS_PENDING -> "待接受";
            case STATUS_EXECUTING -> "执行中";
            case STATUS_PENDING_REVIEW -> "待审核";
            case STATUS_COMPLETED -> "已完成";
            case STATUS_REJECTED -> "拒收";
            case STATUS_RETURNED -> "已驳回";
            default -> "未知";
        };
    }

    public static class AttachmentDownload {
        private final String fileName;
        private final String storedPath;

        public AttachmentDownload(String fileName, String storedPath) {
            this.fileName = fileName;
            this.storedPath = storedPath;
        }

        public String getFileName() {
            return fileName;
        }

        public String getStoredPath() {
            return storedPath;
        }
    }

    @lombok.Data
    public static class DeptEmployeeOption {
        private Long userId;
        private String realName;
        private String username;
    }
}
