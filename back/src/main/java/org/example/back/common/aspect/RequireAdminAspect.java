package org.example.back.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.exception.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 管理员权限校验切面
 * 拦截标记了 @RequireAdmin 注解的方法，校验当前用户是否为管理员
 */
@Aspect
@Component
public class RequireAdminAspect {

    /**
     * 在方法执行前进行权限校验
     */
    @Before("@annotation(org.example.back.common.annotation.RequireAdmin) || @within(org.example.back.common.annotation.RequireAdmin)")
    public void checkAdminPermission(JoinPoint joinPoint) {
        // 1. 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 2. 获取注解
        RequireAdmin requireAdmin = method.getAnnotation(RequireAdmin.class);
        if (requireAdmin == null) {
            requireAdmin = joinPoint.getTarget().getClass().getAnnotation(RequireAdmin.class);
        }

        // 3. 从 Session 中获取用户角色
        Object roleObj = StpUtil.getSession().get("role");
        String role = roleObj == null ? "" : String.valueOf(roleObj).trim().toLowerCase();
        if ("super_admin".equals(role)) {
            role = "superadmin";
        }

        // 4. 校验是否为管理员
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            String message = requireAdmin == null ? "需要管理员权限" : requireAdmin.value();
            throw BusinessException.forbidden(message);
        }
    }
}
