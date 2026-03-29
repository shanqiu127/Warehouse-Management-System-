package org.example.back.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sa-Token 权限接口实现：为框架提供「当前账号拥有哪些角色/权限」的数据来源。
 *
 * 说明：本项目登录成功后会将角色写入 Session：StpUtil.getSession().set("role", role)
 * 因此这里直接从 Sa-Token Session 读取 role 作为角色列表。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired(required = false)
    private SysUserMapper sysUserMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        Object roleObj = session == null ? null : session.get("role");
        String role = normalizeRole(roleObj == null ? null : String.valueOf(roleObj));
        if (role.isEmpty() && sysUserMapper != null) {
            SysUser user = sysUserMapper.selectById(Long.valueOf(String.valueOf(loginId)));
            if (user != null) {
                role = normalizeRole(user.getRole());
                if (!role.isEmpty() && session != null) {
                    session.set("role", role);
                }
            }
        }
        if (role.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(role);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }
}
