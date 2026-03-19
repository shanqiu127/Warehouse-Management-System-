package org.example.back.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.dto.LoginRequest;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.RegisterRequest;
import org.example.back.entity.SysUser;
import org.example.back.common.exception.BusinessException;
import org.example.back.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务类
 */
@Service
public class AuthService {

    private static final String ROLE_superadmin = "superadmin";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_EMPLOYEE = "employee";

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应，包含 token 和用户信息
     */
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = sysUserMapper.selectOne(queryWrapper);

        if (user == null) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw BusinessException.unauthorized("账号已被停用");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        // 角色值做规范化，避免数据库脏数据（空格/大小写）导致权限误判。
        user.setRole(normalizeRole(user.getRole()));

        // 登录时间滚动更新：上次登录 <- 本次登录；本次登录 <- now
        LocalDateTime previousLoginTime = user.getCurrentLoginTime();
        user.setLastLoginTime(previousLoginTime);
        user.setCurrentLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        StpUtil.login(user.getId());

        LoginResponse.UserInfoVO userInfo = buildUserInfoVO(user);
        StpUtil.getSession().set("userInfo", userInfo);
        StpUtil.getSession().set("role", userInfo.getRole());

        String tokenValue = StpUtil.getTokenValue();
        return new LoginResponse(tokenValue, userInfo);
    }

    /**
     * 用户注册（仅允许创建普通用户）
     */
    public void register(RegisterRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, request.getUsername());
        if (sysUserMapper.selectCount(queryWrapper) > 0) {
            throw BusinessException.validateFail("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setRealName(request.getRealName() == null || request.getRealName().isBlank()
                ? request.getUsername()
                : request.getRealName());
        user.setRole(ROLE_EMPLOYEE);
        user.setStatus(1);
        sysUserMapper.insert(user);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    public LoginResponse.UserInfoVO getUserInfo() {
        Object loginId = StpUtil.getLoginIdDefaultNull();

        if (loginId == null) {
            throw BusinessException.unauthorized("用户未登录");
        }

        LoginResponse.UserInfoVO userInfo =
                (LoginResponse.UserInfoVO) StpUtil.getSession().get("userInfo");

        if (userInfo == null) {
            SysUser user = sysUserMapper.selectById((Long) loginId);
            if (user == null) {
                throw BusinessException.notFound("用户不存在");
            }
            userInfo = buildUserInfoVO(user);
            StpUtil.getSession().set("userInfo", userInfo);
        }

        return userInfo;
    }

    /**
     * 用户登出
     */
    public void logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
    }

    /**
     * 构建 UserInfoVO 对象
     *
     * @param user 用户实体
     * @return 用户信息 VO
     */
    private LoginResponse.UserInfoVO buildUserInfoVO(SysUser user) {
        LoginResponse.UserInfoVO vo = new LoginResponse.UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setCurrentLoginTime(user.getCurrentLoginTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }

    /**
     * 检查当前用户是否为管理员
     *
     * @return true\-管理员，false\-非管理员
     */
    public boolean isAdmin() {
        LoginResponse.UserInfoVO userInfo = getUserInfo();
        String role = normalizeRole(userInfo.getRole());
        return ROLE_ADMIN.equals(role) || ROLE_superadmin.equals(role);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toLowerCase();
        if ("super_admin".equals(normalized)) {
            return ROLE_superadmin;
        }
        return normalized;
    }
}
