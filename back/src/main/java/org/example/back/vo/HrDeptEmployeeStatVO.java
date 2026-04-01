package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HrDeptEmployeeStatVO {

    private Long deptId;

    private String deptName;

    private Long employeeCount;

    private BigDecimal ratio;
}