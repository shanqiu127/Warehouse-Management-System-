package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorLogBriefVO {

    private String requestUri;

    private String method;

    private Integer statusCode;

    private String errorType;

    private String message;

    private LocalDateTime createTime;
}
