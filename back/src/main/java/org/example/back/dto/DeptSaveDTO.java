package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptSaveDTO {

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private String leader;

    private String phone;

    private String description;
}