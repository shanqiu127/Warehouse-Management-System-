package org.example.back.common.annotation;

import java.lang.annotation.*;

/**
 * 防止接口短时间重复提交。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreventDuplicateSubmit {

    /**
     * 防抖时间窗口（毫秒）。
     */
    long intervalMs() default 1200L;

    /**
     * 命中重复提交时返回的提示。
     */
    String message() default "请求过于频繁，请稍后再试";
}
