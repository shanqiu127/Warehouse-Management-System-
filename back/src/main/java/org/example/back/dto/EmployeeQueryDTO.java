package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeQueryDTO extends PageQuery {

    private String empName;

    private Long deptId;

    private Integer status;
}