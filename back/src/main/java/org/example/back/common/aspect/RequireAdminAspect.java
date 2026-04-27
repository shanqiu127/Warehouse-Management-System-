package org.example.back.common.aspect;

import org.example.back.common.annotation.RequireAdmin;
import org.example.back.service.AuthzService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 管理员权限校验切面
 * 拦截标记了 @RequireAdmin 注解的方法，校验当前用户是否为管理员
 */
@Aspect
@Component
public class RequireAdminAspect {

    @Autowired
    private AuthzService authzService;

    /**
     * 在使用@RequireAdmin注解的接口方法执行前进行权限校验
     */
    //不管在方法上还是类上，只要有 @RequireAdmin 注解都要进行管理员权限校验
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

        // 3. 统一走认证上下文，避免切面与业务层分别维护角色判断逻辑。
        String message = requireAdmin == null ? "需要管理员权限" : requireAdmin.value();
        authzService.requireAdminOrSuperAdmin(message);
    }
}
