package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.AuditLog;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.ApprovalDecisionDTO;
import org.example.back.dto.DeptQueryDTO;
import org.example.back.dto.DeptSaveDTO;
import org.example.back.service.DeptService;
import org.example.back.vo.DeptVO;
import org.example.back.vo.OptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/depts")
@RequireAdmin("仅管理员可访问部门管理")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping("/page")
    public Result<PageResult<DeptVO>> page(DeptQueryDTO queryDTO) {
        return Result.success(deptService.page(queryDTO));
    }

    @GetMapping("/options")
    public Result<List<OptionVO>> options() {
        return Result.success(deptService.options());
    }

    @GetMapping("/{id}")
    public Result<DeptVO> getById(@PathVariable Long id) {
        return Result.success(deptService.getById(id));
    }

    @PostMapping
    @PreventDuplicateSubmit(message = "请勿重复提交部门新增请求")
    public Result<Void> create(@Valid @RequestBody DeptSaveDTO dto) {
        deptService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreventDuplicateSubmit(message = "请勿重复提交部门编辑请求")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeptSaveDTO dto) {
        deptService.update(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/approve")
    @AuditLog(module = "部门管理", action = "审批通过", targetType = "部门申请")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "请勿重复审批")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody(required = false) ApprovalDecisionDTO dto) {
        deptService.approve(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @AuditLog(module = "部门管理", action = "审批驳回", targetType = "部门申请")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "请勿重复审批")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody(required = false) ApprovalDecisionDTO dto) {
        deptService.reject(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success();
    }
}