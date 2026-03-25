package org.example.back.common.annotation;

import java.lang.annotation.*;

/**
 * 操作审计注解
 * 标记关键业务接口，由审计切面统一落库。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 业务模块，例如：用户管理、进货管理。
     */
    String module();

    /**
     * 操作动作，例如：删除、作废并红冲、重置密码。
     */
    String action();

    /**
     * 目标类型，例如：用户、进货单。
     */
    String targetType() default "";
}
