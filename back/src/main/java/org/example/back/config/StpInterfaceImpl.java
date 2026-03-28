package org.example.back.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限接口实现：为框架提供「当前账号拥有哪些角色/权限」的数据来源。
 *
 * 说明：本项目登录成功后会将角色写入 Session：StpUtil.getSession().set("role", role)
 * 因此这里直接从 Sa-Token Session 读取 role 作为角色列表。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

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
        String role = roleObj == null ? "" : String.valueOf(roleObj).trim().toLowerCase();
        if (role.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(role);
    }
}
