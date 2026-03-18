package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_error_log")
public class SysErrorLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String requestUri;

    private String method;

    private Integer statusCode;

    private String errorType;

    private String message;

    private LocalDateTime createTime;
}
