package org.example.back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 用户信息
     */
    private UserInfoVO userInfo;

    /**
     * 用户信息 VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoVO {
        /**
         * 用户ID
         */
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 真实姓名
         */
        private String realName;

        /**
         * 角色：admin-管理员，employee-普通员工，superadmin-超级管理员
         */
        private String role;

        /**
         * 本次登录时间
         */
        private LocalDateTime currentLoginTime;

        /**
         * 上次登录时间
         */
        private LocalDateTime lastLoginTime;
    }
}
