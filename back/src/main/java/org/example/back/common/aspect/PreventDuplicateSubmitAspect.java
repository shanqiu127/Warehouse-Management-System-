package org.example.back.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.exception.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 防重复提交切面：在固定窗口内拦截同一用户的同类重复写请求。
 */
@Aspect
@Component
public class PreventDuplicateSubmitAspect {

    private static final int MAX_CACHE_SIZE = 10000;

    private final Map<String, Long> requestWindowMap = new ConcurrentHashMap<>();

    @Before("@annotation(preventDuplicateSubmit)")
    public void preventDuplicateSubmit(JoinPoint joinPoint, PreventDuplicateSubmit preventDuplicateSubmit) {
        long now = System.currentTimeMillis();
        long intervalMs = preventDuplicateSubmit.intervalMs();
        String requestKey = buildRequestKey(joinPoint);

        requestWindowMap.compute(requestKey, (key, lastTime) -> {
            if (lastTime != null && now - lastTime < intervalMs) {
                throw BusinessException.validateFail(preventDuplicateSubmit.message());
            }
            return now;
        });

        cleanupIfNecessary(now, intervalMs);
    }

    private String buildRequestKey(JoinPoint joinPoint) {
        HttpServletRequest request = currentRequest();
        String path = request == null ? "unknown-path" : request.getRequestURI();
        String method = request == null ? "UNKNOWN" : request.getMethod();
        String actor = resolveActor(request);
        String argsFingerprint = fingerprintArgs(joinPoint.getArgs());

        return actor + "|" + method + "|" + path + "|" + argsFingerprint;
    }

    private String resolveActor(HttpServletRequest request) {
        if (StpUtil.isLogin()) {
            return "uid:" + StpUtil.getLoginIdAsString();
        }
        if (request != null) {
            return "ip:" + request.getRemoteAddr();
        }
        return "anonymous";
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String fingerprintArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "no-args";
        }
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof BindingResult))
                .filter(arg -> !(arg instanceof MultipartFile))
                .map(Object::toString)
                .collect(Collectors.joining("|"));
    }

    private void cleanupIfNecessary(long now, long intervalMs) {
        if (requestWindowMap.size() < MAX_CACHE_SIZE) {
            return;
        }
        long expireThreshold = now - Math.max(intervalMs, 5000L);
        requestWindowMap.entrySet().removeIf(entry -> entry.getValue() < expireThreshold);
    }
}
