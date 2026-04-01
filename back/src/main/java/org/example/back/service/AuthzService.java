package org.example.back.service;

import org.example.back.common.exception.BusinessException;
import org.example.back.dto.LoginResponse;
import org.example.back.entity.SysDept;
import org.example.back.mapper.SysDeptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

@Service
public class AuthzService {

    public static final String ROLE_SUPERADMIN = "superadmin";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_EMPLOYEE = "employee";
    public static final String DEPT_FINANCE = "finance";
    public static final String DEPT_SALES = "sales";
    public static final String DEPT_WAREHOUSE = "warehouse";
    public static final String DEPT_PURCHASE = "purchase";
    public static final String DEPT_HR = "hr";
    public static final String DEPT_SYSTEM_MANAGEMENT = "system_management";

    @Autowired
    private AuthService authService;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    public LoginResponse.UserInfoVO currentUser() {
        return authService.getUserInfo();
    }

    public String currentRole() {
        return normalizeRole(currentUser().getRole());
    }

    public Long currentDeptId() {
        return currentUser().getDeptId();
    }

    public String currentDeptCode() {
        return normalizeDeptCode(currentUser().getDeptCode());
    }

    public boolean isSuperAdmin() {
        return ROLE_SUPERADMIN.equals(currentRole());
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(currentRole());
    }

    public boolean isEmployee() {
        return ROLE_EMPLOYEE.equals(currentRole());
    }

    public boolean isAdminOrSuperAdmin() {
        return isAdmin() || isSuperAdmin();
    }

    public boolean hasDeptAccess(Long targetDeptId) {
        if (isSuperAdmin()) {
            return true;
        }
        if (targetDeptId == null) {
            return false;
        }
        Long currentDeptId = currentDeptId();
        return currentDeptId != null && currentDeptId.equals(targetDeptId);
    }

    public boolean isDeptAdmin(String deptCode) {
        return isAdmin() && normalizeDeptCode(deptCode).equals(currentDeptCode());
    }

    public boolean isHrAdmin() {
        return isDeptAdmin(DEPT_HR);
    }

    public boolean hasDeptAdminOrSuperAdminAccess(String deptCode) {
        return isSuperAdmin() || isDeptAdmin(deptCode);
    }

    public void requireAdminOrSuperAdmin(String message) {
        if (!isAdminOrSuperAdmin()) {
            throw BusinessException.forbidden(message);
        }
    }

    public void requireSuperAdmin(String message) {
        if (!isSuperAdmin()) {
            throw BusinessException.forbidden(message);
        }
    }

    public void requireCurrentDept(Long targetDeptId, String message) {
        if (!hasDeptAccess(targetDeptId)) {
            throw BusinessException.forbidden(message);
        }
    }

    public void requireDeptAdmin(String deptCode, String message) {
        if (!isDeptAdmin(deptCode)) {
            throw BusinessException.forbidden(message);
        }
    }

    public void requireDeptAdminOrSuperAdmin(String deptCode, String message) {
        if (!hasDeptAdminOrSuperAdminAccess(deptCode)) {
            throw BusinessException.forbidden(message);
        }
    }

    public void requireAnyDeptAdminOrSuperAdmin(Collection<String> deptCodes, String message) {
        if (isSuperAdmin()) {
            return;
        }
        String currentDeptCode = currentDeptCode();
        boolean matched = deptCodes.stream().map(this::normalizeDeptCode).anyMatch(currentDeptCode::equals);
        if (!isAdmin() || !matched) {
            throw BusinessException.forbidden(message);
        }
    }

    public void requireAnyDeptAdminOrSuperAdmin(String message, String... deptCodes) {
        requireAnyDeptAdminOrSuperAdmin(Arrays.asList(deptCodes), message);
    }

    public SysDept requireDept(Long deptId) {
        if (deptId == null) {
            throw BusinessException.validateFail("所属部门不能为空");
        }
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            throw BusinessException.validateFail("所属部门不存在");
        }
        return dept;
    }

    public String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeDeptCode(String deptCode) {
        if (!StringUtils.hasText(deptCode)) {
            return "";
        }
        return deptCode.trim().toLowerCase(Locale.ROOT);
    }

    public String currentOperatorLabel() {
        LoginResponse.UserInfoVO userInfo = currentUser();
        String role = normalizeRole(userInfo.getRole());
        String deptCode = normalizeDeptCode(userInfo.getDeptCode());
        if (ROLE_SUPERADMIN.equals(role)) {
            return "超级管理员";
        }
        if (ROLE_ADMIN.equals(role) && DEPT_HR.equals(deptCode)) {
            return "人事部管理员";
        }
        if (ROLE_ADMIN.equals(role)) {
            return StringUtils.hasText(userInfo.getDeptName()) ? userInfo.getDeptName() + "管理员" : "部门管理员";
        }
        return StringUtils.hasText(userInfo.getRealName()) ? userInfo.getRealName() : "系统";
    }
}