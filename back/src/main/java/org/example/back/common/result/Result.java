package org.example.back.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果类
 * 匹配前端 request.js 解析习惯: { "code": 200, "msg": "成功", "data": ... }
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "成功", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    /**
     * 失败响应（默认消息）
     */
    public static <T> Result<T> fail() {
        return new Result<>(500, "操作失败", null);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    /**
     * 失败响应（自定义状态码和消息）
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    /**
     * 参数校验失败
     */
    public static <T> Result<T> validateFail() {
        return fail(400, "参数校验失败");
    }

    /**
     * 参数校验失败（自定义消息）
     */
    public static <T> Result<T> validateFail(String msg) {
        return fail(400, msg);
    }

    /**
     * 未授权
     */
    public static <T> Result<T> unauthorized() {
        return fail(401, "请先登录");
    }

    /**
     * 未授权（自定义消息）
     */
    public static <T> Result<T> unauthorized(String msg) {
        return fail(401, msg);
    }

    /**
     * 无权限
     */
    public static <T> Result<T> forbidden() {
        return fail(403, "您没有权限执行此操作");
    }

    /**
     * 无权限（自定义消息）
     */
    public static <T> Result<T> forbidden(String msg) {
        return fail(403, msg);
    }

    /**
     * 资源不存在
     */
    public static <T> Result<T> notFound() {
        return fail(404, "资源不存在");
    }

    /**
     * 资源不存在（自定义消息）
     */
    public static <T> Result<T> notFound(String msg) {
        return fail(404, msg);
    }
}
