package org.example.back.service;

import cn.hutool.crypto.digest.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.dto.UserQueryDTO;
import org.example.back.dto.UserSaveDTO;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysUserMapper;
import org.example.back.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserManageService {

    private static final String DEFAULT_PASSWORD = "ljs2005416LJS@";
    private static final String ROLE_superadmin = "superadmin";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_EMPLOYEE = "employee";

    @Autowired
    private SysUserMapper sysUserMapper;

    public PageResult<UserVO> page(UserQueryDTO queryDTO) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getUsername()), SysUser::getUsername, queryDTO.getUsername())
                .eq(StringUtils.hasText(queryDTO.getRole()), SysUser::getRole, queryDTO.getRole())
                .eq(queryDTO.getStatus() != null, SysUser::getStatus, queryDTO.getStatus())
                .orderByDesc(SysUser::getId);

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<UserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public UserVO getById(Long id) {
        return toVO(requireUser(id));
    }

    public void create(UserSaveDTO dto) {
        validateRoleForManage(dto.getRole());
        checkUsernameUnique(dto.getUsername(), null);
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
        sysUserMapper.insert(user);
    }

    public void update(Long id, UserSaveDTO dto) {
        SysUser user = requireUser(id);
        validateRoleForManage(dto.getRole());
        checkUsernameUnique(dto.getUsername(), id);
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus() == null ? user.getStatus() : dto.getStatus());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        sysUserMapper.updateById(user);
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.validateFail("用户状态只能为 0 或 1");
        }
        SysUser user = requireUser(id);
        if (ROLE_superadmin.equals(user.getRole())) {
            throw BusinessException.validateFail("超级管理员状态不允许修改");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    public void delete(Long id) {
        SysUser user = requireUser(id);
        if (ROLE_superadmin.equals(user.getRole())) {
            throw BusinessException.validateFail("超级管理员账号不允许删除");
        }
        if ("admin".equals(user.getUsername())) {
            throw BusinessException.validateFail("默认管理员账号不允许删除");
        }
        sysUserMapper.deleteById(id);
    }

    public void resetPassword(Long targetUserId, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw BusinessException.validateFail("新密码至少 6 位");
        }

        SysUser operator = requireCurrentUser();
        SysUser targetUser = requireUser(targetUserId);

        String operatorRole = operator.getRole();
        String targetRole = targetUser.getRole();

        if (ROLE_superadmin.equals(operatorRole)) {
            if (ROLE_superadmin.equals(targetRole)) {
                throw BusinessException.validateFail("超级管理员账号不允许通过该接口修改密码");
            }
        } else if (ROLE_ADMIN.equals(operatorRole)) {
            if (!ROLE_EMPLOYEE.equals(targetRole)) {
                throw BusinessException.forbidden("普通管理员仅可重置普通用户密码");
            }
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

    private void validateRoleForManage(String role) {
        if (!ROLE_ADMIN.equals(role) && !ROLE_EMPLOYEE.equals(role)) {
            throw BusinessException.validateFail("用户角色仅支持 admin 或 employee");
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

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus() != null && user.getStatus() == 1);
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}