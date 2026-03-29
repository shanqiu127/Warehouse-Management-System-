package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeVO {

    private Long id;

    private String title;

    private String content;

    private String targetRole;

    private Long targetDeptId;

    private String targetDeptName;

    private String publisher;

    private String author;

    private LocalDateTime publishTime;

    private LocalDateTime date;

    private Integer status;

    private LocalDateTime createTime;
}