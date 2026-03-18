package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private Boolean status;

    private String phone;

    private String email;

    private LocalDateTime createTime;
}