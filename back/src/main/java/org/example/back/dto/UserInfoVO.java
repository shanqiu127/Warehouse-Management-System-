package org.example.back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色：admin-管理员，employee-普通员工，superadmin-超级管理员
     */
    private String role;
}
