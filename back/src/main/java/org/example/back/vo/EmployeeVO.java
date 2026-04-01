package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeVO {

    private Long id;

    private String username;

    private String empCode;

    private String empName;

    private String gender;

    private Long deptId;

    private String deptName;

    private String position;

    private String phone;

    private String email;

    private Integer status;

    private Boolean readOnly;

    private LocalDateTime createTime;
}