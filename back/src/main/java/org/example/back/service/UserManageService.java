package org.example.back.service;

import cn.hutool.crypto.digest.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.UserQueryDTO;
import org.example.back.dto.UserSaveDTO;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysUserMapper;
import org.example.back.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserManageService {

    private static final String DEFAULT_PASSWORD = "123456";
    private static final String ROLE_SUPERADMIN = "superadmin";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_EMPLOYEE = "employee";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private AuthzService authzService;

    public PageResult<UserVO> page(UserQueryDTO queryDTO) {
        LoginResponse.UserInfoVO operator = authzService.currentUser();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getUsername()), SysUser::getUsername, queryDTO.getUsername())
                .eq(StringUtils.hasText(queryDTO.getRole()), SysUser::getRole, queryDTO.getRole())
                .eq(queryDTO.getDeptId() != null, SysUser::getDeptId, queryDTO.getDeptId())
                .eq(queryDTO.getStatus() != null, SysUser::getStatus, queryDTO.getStatus())
                .orderByDesc(SysUser::getId);

        if (authzService.isAdmin()) {
            wrapper.eq(SysUser::getRole, ROLE_EMPLOYEE)
                    .eq(SysUser::getDeptId, operator.getDeptId());
        }

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        Map<Long, SysDept> deptMap = buildDeptMap(page.getRecords().stream()
                .map(SysUser::getDeptId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        List<UserVO> records = page.getRecords().stream().map(item -> toVO(item, deptMap.get(item.getDeptId()))).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public UserVO getById(Long id) {
        SysUser user = requireManageableUser(id);
        return toVO(user, user.getDeptId() == null ? null : sysDeptMapper.selectById(user.getDeptId()));
    }

    public void create(UserSaveDTO dto) {
        LoginResponse.UserInfoVO operator = authzService.currentUser();
        validateRoleForManage(dto.getRole());
        rejectSuperadminCreate(dto.getRole());
        Long deptId = validateAndResolveDeptId(dto.getRole(), dto.getDeptId(), operator);
        checkUsernameUnique(dto.getUsername(), null);
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setDeptId(deptId);
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
        sysUserMapper.insert(user);
    }

    public void update(Long id, UserSaveDTO dto) {
        LoginResponse.UserInfoVO operator = authzService.currentUser();
        SysUser user = requireManageableUser(id);
        validateRoleForManage(dto.getRole());
        validateSuperadminRoleChange(user.getRole(), dto.getRole());
        Long deptId = validateAndResolveDeptId(dto.getRole(), dto.getDeptId(), operator);
        checkUsernameUnique(dto.getUsername(), id);
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole());
        user.setDeptId(deptId);
        user.setStatus(dto.getStatus() == null ? user.getStatus() : dto.getStatus());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        sysUserMapper.updateById(user);
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.validateFail("用户状态只能为 0 或 1");
        }
        SysUser user = requireManageableUser(id);
        if (ROLE_SUPERADMIN.equals(user.getRole())) {
            throw BusinessException.validateFail("超级管理员状态不允许修改");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    public void delete(Long id) {
        SysUser user = requireManageableUser(id);
        if (ROLE_SUPERADMIN.equals(user.getRole())) {
            throw BusinessException.validateFail("超级管理员账号不允许删除");
        }
        sysUserMapper.deleteById(id);
    }

    public void resetPassword(Long targetUserId, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw BusinessException.validateFail("新密码至少 6 位");
        }

        SysUser operator = requireCurrentUser();
        SysUser targetUser = requireManageableUser(targetUserId);

        String operatorRole = operator.getRole();
        String targetRole = targetUser.getRole();

        if (ROLE_SUPERADMIN.equals(operatorRole)) {
            if (ROLE_SUPERADMIN.equals(targetRole)) {
                throw BusinessException.validateFail("超级管理员账号不允许通过该接口修改密码");
            }
        } else if (ROLE_ADMIN.equals(operatorRole)) {
            assertAdminCanManageUser(targetUser);
        } else {
            throw BusinessException.forbidden("当前角色无权重置密码");
        }

        targetUser.setPassword(BCrypt.hashpw(newPassword));
        sysUserMapper.updateById(targetUser);
    }

    private SysUser requireCurrentUser() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            throw BusinessException.unauthorized("用户未登录");
        }
        Long userId = Long.valueOf(String.valueOf(loginId));
        return requireUser(userId);
    }

    private SysUser requireManageableUser(Long id) {
        SysUser user = requireUser(id);
        if (authzService.isSuperAdmin()) {
            return user;
        }
        assertAdminCanManageUser(user);
        return user;
    }

    private void validateRoleForManage(String role) {
        if (!ROLE_SUPERADMIN.equals(role) && !ROLE_ADMIN.equals(role) && !ROLE_EMPLOYEE.equals(role)) {
            throw BusinessException.validateFail("用户角色仅支持 superadmin、admin 或 employee");
        }
    }

    private Long validateAndResolveDeptId(String targetRole, Long deptId, LoginResponse.UserInfoVO operator) {
        String normalizedRole = authzService.normalizeRole(targetRole);
        if (ROLE_SUPERADMIN.equals(normalizedRole)) {
            return null;
        }

        SysDept dept = authzService.requireDept(deptId);
        if (authzService.isAdmin()) {
            if (!ROLE_EMPLOYEE.equals(normalizedRole)) {
                throw BusinessException.forbidden("部门管理员仅可创建或维护本部门员工账号");
            }
            if (operator.getDeptId() == null || !operator.getDeptId().equals(dept.getId())) {
                throw BusinessException.forbidden("部门管理员仅可操作本部门员工账号");
            }
        }
        return dept.getId();
    }

    private void rejectSuperadminCreate(String role) {
        if (ROLE_SUPERADMIN.equals(role)) {
            throw BusinessException.validateFail("超级管理员账号仅允许系统初始化，不支持在用户管理中新增");
        }
    }

    private void validateSuperadminRoleChange(String oldRole, String newRole) {
        if (!StringUtils.hasText(oldRole) || !StringUtils.hasText(newRole)) {
            return;
        }
        if (ROLE_SUPERADMIN.equals(oldRole) && !ROLE_SUPERADMIN.equals(newRole)) {
            throw BusinessException.validateFail("超级管理员角色不允许变更");
        }
        if (!ROLE_SUPERADMIN.equals(oldRole) && ROLE_SUPERADMIN.equals(newRole)) {
            throw BusinessException.validateFail("普通账号不允许修改为超级管理员");
        }
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId);
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("用户名已存在");
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    private void assertAdminCanManageUser(SysUser user) {
        if (!ROLE_EMPLOYEE.equals(authzService.normalizeRole(user.getRole()))) {
            throw BusinessException.forbidden("部门管理员仅可操作本部门员工账号");
        }
        authzService.requireCurrentDept(user.getDeptId(), "部门管理员仅可操作本部门员工账号");
    }

    private Map<Long, SysDept> buildDeptMap(Set<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysDept::getId, deptIds);
        return sysDeptMapper.selectList(wrapper).stream().collect(Collectors.toMap(SysDept::getId, Function.identity()));
    }

    private UserVO toVO(SysUser user, SysDept dept) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setDeptId(user.getDeptId());
        vo.setDeptCode(dept == null ? null : dept.getDeptCode());
        vo.setDeptName(dept == null ? null : dept.getDeptName());
        vo.setStatus(user.getStatus() != null && user.getStatus() == 1);
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}