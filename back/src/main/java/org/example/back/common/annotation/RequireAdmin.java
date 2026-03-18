package org.example.back.common.annotation;

import java.lang.annotation.*;

/**
 * 需要管理员权限的注解
 * 标记在接口方法上，表示该接口需要管理员角色才能访问
 *
 * 使用方式：
 * <pre>
 * {@code
 * @PostMapping("/create")
 * @RequireAdmin
 * public Result<Void> create(...) {
 *     // 只有 admin 角色可以访问
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {

    /**
     * 权限描述，用于日志记录和提示
     */
    String value() default "需要管理员权限";
}
