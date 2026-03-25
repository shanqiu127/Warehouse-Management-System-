package org.example.back.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.back.common.annotation.AuditLog;
import org.example.back.common.util.ClientIpUtil;
import org.example.back.entity.SysOperationLog;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysOperationLogMapper;
import org.example.back.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作审计切面：拦截带 @AuditLog 的方法并记录操作日志。
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @AfterReturning("@annotation(auditLog)")
    public void recordOperationLog(JoinPoint joinPoint, AuditLog auditLog) {
        try {
            HttpServletRequest request = currentRequest();

            SysOperationLog logEntity = new SysOperationLog();
            logEntity.setModule(auditLog.module());
            logEntity.setAction(auditLog.action());
            logEntity.setTargetType(auditLog.targetType());
            logEntity.setTargetId(resolveTargetId(joinPoint.getArgs()));
            logEntity.setRequestUri(request == null ? "" : request.getRequestURI());
            logEntity.setIp(ClientIpUtil.getClientIp(request));
            logEntity.setCreateTime(LocalDateTime.now());

            fillOperator(logEntity);
            sysOperationLogMapper.insert(logEntity);
        } catch (Exception ex) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getDeclaringTypeName() + "#" + signature.getMethod().getName();
            log.warn("审计日志记录失败, method={}, error={}", methodName, ex.getMessage());
        }
    }

    private void fillOperator(SysOperationLog logEntity) {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            logEntity.setUsername("anonymous");
            return;
        }
        Long userId = Long.valueOf(String.valueOf(loginId));
        logEntity.setUserId(userId);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            logEntity.setUsername("uid:" + userId);
            return;
        }
        logEntity.setUsername(user.getUsername());
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String resolveTargetId(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        for (Object arg : args) {
            if (arg instanceof Long || arg instanceof Integer) {
                return String.valueOf(arg);
            }
        }
        for (Object arg : args) {
            if (arg instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return "";
    }
}
