package org.example.back.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
// MyBatis-Plus自动填充功能的实现类，用于在插入和更新操作时自动设置createTime和updateTime字段的值
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        if (getFieldValByName("createTime", metaObject) == null) {
            setFieldValByName("createTime", now, metaObject);
        }
        if (getFieldValByName("updateTime", metaObject) == null) {
            setFieldValByName("updateTime", now, metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}
