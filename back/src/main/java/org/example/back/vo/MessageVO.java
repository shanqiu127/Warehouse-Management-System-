package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

    private Long id;

    private String title;

    private String content;

    private Boolean read;

    private LocalDateTime readTime;

    private LocalDateTime createTime;
}