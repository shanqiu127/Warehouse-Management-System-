package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.AuditLog;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.ApprovalCreateDTO;
import org.example.back.dto.ApprovalDecisionDTO;
import org.example.back.dto.ApprovalQueryDTO;
import org.example.back.service.ApprovalService;
import org.example.back.vo.ApprovalOrderVO;
import org.example.back.vo.ReminderSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/approval-orders")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @PostMapping
    @RequireAdmin("仅管理员可提交作废审批申请")
    @PreventDuplicateSubmit(intervalMs = 1200, message = "请勿重复提交审批申请")
    public Result<Void> create(@Valid @RequestBody ApprovalCreateDTO dto) {
        approvalService.create(dto);
        return Result.success();
    }

    @GetMapping("/page")
    @RequireAdmin("仅管理员可查看审批列表")
    public Result<PageResult<ApprovalOrderVO>> page(ApprovalQueryDTO queryDTO) {
        return Result.success(approvalService.page(queryDTO));
    }

    @GetMapping("/pending-count")
    @RequireAdmin("仅管理员可查看待审批数量")
    public Result<Long> pendingCount() {
        return Result.success(approvalService.pendingCount());
    }

    @GetMapping("/pending-reminder")
    @RequireAdmin("仅管理员可查看待审批提醒")
    public Result<ReminderSummaryVO> pendingReminder() {
        return Result.success(approvalService.pendingReminder());
    }

    @PutMapping("/{id}/approve")
    @RequireAdmin("仅管理员可审批")
    @AuditLog(module = "作废审批", action = "审批通过", targetType = "审批单")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "请勿重复审批")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody(required = false) ApprovalDecisionDTO dto) {
        approvalService.approve(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @RequireAdmin("仅管理员可审批")
    @AuditLog(module = "作废审批", action = "审批驳回", targetType = "审批单")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "请勿重复审批")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody(required = false) ApprovalDecisionDTO dto) {
        approvalService.reject(id, dto);
        return Result.success();
    }
}
