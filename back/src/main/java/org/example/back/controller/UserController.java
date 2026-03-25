package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.AuditLog;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.UserQueryDTO;
import org.example.back.dto.UserResetPasswordDTO;
import org.example.back.dto.UserSaveDTO;
import org.example.back.dto.UserStatusDTO;
import org.example.back.service.UserManageService;
import org.example.back.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/users")
@RequireAdmin("仅管理员可访问用户管理")
public class UserController {

    @Autowired
    private UserManageService userManageService;

    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(UserQueryDTO queryDTO) {
        return Result.success(userManageService.page(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userManageService.getById(id));
    }

    @PostMapping
    @PreventDuplicateSubmit(message = "请勿重复提交用户新增请求")
    public Result<Void> create(@Valid @RequestBody UserSaveDTO dto) {
        userManageService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreventDuplicateSubmit(message = "请勿重复提交用户编辑请求")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserSaveDTO dto) {
        userManageService.update(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreventDuplicateSubmit(intervalMs = 800, message = "状态更新过快，请稍后再试")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userManageService.updateStatus(id, dto.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "用户管理", action = "删除", targetType = "用户")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        userManageService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @AuditLog(module = "用户管理", action = "重置密码", targetType = "用户")
    @PreventDuplicateSubmit(intervalMs = 1500, message = "请勿重复提交重置密码请求")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserResetPasswordDTO dto) {
        userManageService.resetPassword(id, dto.getNewPassword());
        return Result.success();
    }
}