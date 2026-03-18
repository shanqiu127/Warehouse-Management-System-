package org.example.back.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况，如库存不足、参数校验失败等
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误状态码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String msg;

    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
        this.msg = msg;
    }

    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(String msg, Throwable cause) {
        super(msg, cause);
        this.code = 500;
        this.msg = msg;
    }

    public BusinessException(Integer code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }

    // ==================== 常用异常构造方法 ====================

    /**
     * 参数校验失败
     */
    public static BusinessException validateFail(String msg) {
        return new BusinessException(400, msg);
    }

    /**
     * 未授权
     */
    public static BusinessException unauthorized(String msg) {
        return new BusinessException(401, msg);
    }

    /**
     * 无权限
     */
    public static BusinessException forbidden(String msg) {
        return new BusinessException(403, msg);
    }

    /**
     * 资源不存在
     */
    public static BusinessException notFound(String msg) {
        return new BusinessException(404, msg);
    }

    /**
     * 库存不足
     */
    public static BusinessException stockInsufficient() {
        return new BusinessException("抱歉，当前商品库存不足无法进行出库操作");
    }

    /**
     * 库存不足（自定义消息）
     */
    public static BusinessException stockInsufficient(String msg) {
        return new BusinessException(400, msg);
    }
}
