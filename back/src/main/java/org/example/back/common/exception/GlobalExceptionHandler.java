package org.example.back.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.back.entity.SysErrorLog;
import org.example.back.mapper.SysErrorLogMapper;
import org.example.back.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 捕获所有异常并统一返回格式，使前端统一利用 ElMessage 弹出红色提醒
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private SysErrorLogMapper sysErrorLogMapper;

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: URI={}, Code={}, Message={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("未登录异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.unauthorized("用户未登录，请先登录");
    }

    /**
     * 处理 Sa-Token 角色校验失败异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        log.warn("角色权限异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.forbidden("权限不足，需要管理员权限");
    }

    /**
     * 处理 Sa-Token 权限校验失败异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.warn("权限校验异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.forbidden("权限不足，无法执行此操作");
    }

    /**
     * 处理参数校验异常 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        return Result.validateFail(errorMsg);
    }

    /**
     * 处理参数绑定异常 (@Validated)
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e, HttpServletRequest request) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        return Result.validateFail(errorMsg);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.validateFail(e.getMessage());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: URI={}", request.getRequestURI(), e);
        recordErrorLog(request, e, 500);
        return Result.fail("系统异常，请稍后重试");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常: URI={}, Message={}", request.getRequestURI(), e.getMessage(), e);
        recordErrorLog(request, e, 500);
        return Result.fail("系统异常，请稍后重试");
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: URI={}, Message={}", request.getRequestURI(), e.getMessage(), e);
        recordErrorLog(request, e, 500);
        return Result.fail("系统异常，请稍后重试");
    }
    // 记录错误日志到数据库
    private void recordErrorLog(HttpServletRequest request, Throwable error, Integer statusCode) {
        try {
            SysErrorLog logEntity = new SysErrorLog();
            logEntity.setRequestUri(request == null ? null : request.getRequestURI());
            logEntity.setMethod(request == null ? null : request.getMethod());
            logEntity.setStatusCode(statusCode);
            logEntity.setErrorType(error == null ? "UnknownException" : error.getClass().getSimpleName());
            logEntity.setMessage(error == null ? "" : safeMessage(error.getMessage()));
            logEntity.setCreateTime(LocalDateTime.now());
            sysErrorLogMapper.insert(logEntity);
        } catch (Exception ex) {
            log.warn("写入 sys_error_log 失败: {}", ex.getMessage());
        }
    }
    // 为了防止日志过长导致数据库存储问题，截取前 500 字符
    private String safeMessage(String msg) {
        if (msg == null) {
            return "";
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
