package org.example.back.vo;

import lombok.Data;

import java.util.List;

@Data
public class HrEmployeeChartVO {

    private Long totalEmployeeCount;

    private Long deptCount;

    private Long occupiedDeptCount;

    private String topDeptName;

    private Long topDeptEmployeeCount;

    private List<HrDeptEmployeeStatVO> deptStats;
}