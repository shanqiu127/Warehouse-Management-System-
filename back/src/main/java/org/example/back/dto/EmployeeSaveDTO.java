package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeeSaveDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "员工姓名不能为空")
    private String empName;

    @NotNull(message = "所属部门不能为空")
    private Long deptId;

    private String position;

    private String phone;

    private String email;

    private Integer status;
}