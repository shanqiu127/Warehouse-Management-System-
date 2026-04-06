package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.WorkRequirementCreateDTO;
import org.example.back.dto.WorkRequirementExecuteDTO;
import org.example.back.dto.WorkRequirementQueryDTO;
import org.example.back.dto.WorkRequirementReviewDTO;
import org.example.back.service.WorkRequirementService;
import org.example.back.vo.ReminderSummaryVO;
import org.example.back.vo.WorkRequirementAssignVO;
import org.example.back.vo.WorkRequirementDetailVO;
import org.example.back.vo.WorkRequirementVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkRequirementController {

    @Autowired
    private WorkRequirementService workRequirementService;

    // ========== 管理员端 ==========

    @GetMapping("/system/work-requirements/page")
    @RequireAdmin("仅管理员可查看工作要求列表")
    public Result<PageResult<WorkRequirementVO>> page(WorkRequirementQueryDTO queryDTO) {
        return Result.success(workRequirementService.page(queryDTO));
    }

    @GetMapping("/system/work-requirements/pending-review-reminder")
    @RequireAdmin("仅管理员可查看工作要求待审核提醒")
    public Result<ReminderSummaryVO> pendingReviewReminder() {
        return Result.success(workRequirementService.pendingReviewReminder());
    }

    @GetMapping("/system/work-requirements/overdue-reminder")
    @RequireAdmin("仅管理员可查看工作要求超时提醒")
    public Result<ReminderSummaryVO> overdueReminder() {
        return Result.success(workRequirementService.overdueReminder());
    }

    @GetMapping("/system/work-requirements/{id}")
    @RequireAdmin("仅管理员可查看工作要求详情")
    public Result<WorkRequirementDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(workRequirementService.getDetail(id));
    }

    @PostMapping("/system/work-requirements")
    @RequireAdmin("仅管理员可创建工作要求")
    @PreventDuplicateSubmit(message = "请勿重复提交工作要求")
    public Result<Void> create(@Valid @RequestBody WorkRequirementCreateDTO dto) {
        workRequirementService.create(dto);
        return Result.success();
    }

    @DeleteMapping("/system/work-requirements/{id}")
    @RequireAdmin("仅管理员可删除工作要求")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "删除请求过于频繁")
    public Result<Void> delete(@PathVariable Long id) {
        workRequirementService.delete(id);
        return Result.success();
    }

    @PutMapping("/system/work-requirements/assign/{assignId}/review")
    @RequireAdmin("仅管理员可审核工作要求")
    @PreventDuplicateSubmit(message = "请勿重复提交审核")
    public Result<Void> review(@PathVariable Long assignId, @Valid @RequestBody WorkRequirementReviewDTO dto) {
        workRequirementService.review(assignId, dto.getApproved());
        return Result.success();
    }

    @GetMapping("/system/work-requirements/dept-employees")
    @RequireAdmin("仅管理员可获取部门员工列表")
    public Result<List<WorkRequirementService.DeptEmployeeOption>> getDeptEmployees() {
        return Result.success(workRequirementService.getDeptEmployees());
    }

    // ========== 员工端 ==========

    @GetMapping("/home/work-requirements")
    public Result<List<WorkRequirementAssignVO>> getMyRequirements() {
        return Result.success(workRequirementService.getMyRequirements());
    }

    @GetMapping("/home/work-requirements/{assignId}")
    public Result<WorkRequirementAssignVO> getAssignDetail(@PathVariable Long assignId) {
        return Result.success(workRequirementService.getAssignDetail(assignId));
    }

    @PutMapping("/home/work-requirements/{assignId}/accept")
    @PreventDuplicateSubmit(message = "请勿重复操作")
    public Result<Void> accept(@PathVariable Long assignId) {
        workRequirementService.accept(assignId);
        return Result.success();
    }

    @PutMapping("/home/work-requirements/{assignId}/reject")
    @PreventDuplicateSubmit(message = "请勿重复操作")
    public Result<Void> rejectAssign(@PathVariable Long assignId) {
        workRequirementService.reject(assignId);
        return Result.success();
    }

    @PutMapping("/home/work-requirements/{assignId}/submit")
    @PreventDuplicateSubmit(message = "请勿重复提交执行结果")
    public Result<Void> submitExecution(@PathVariable Long assignId, @Valid @RequestBody WorkRequirementExecuteDTO dto) {
        dto.setAssignId(assignId);
        workRequirementService.submitExecution(dto);
        return Result.success();
    }
}
