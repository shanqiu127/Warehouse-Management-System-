package org.example.back.controller;

import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.Result;
import org.example.back.service.HrChartService;
import org.example.back.vo.HrEmployeeChartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/hr-charts")
@RequireAdmin("仅管理员可访问员工图表")
public class HrChartController {

    @Autowired
    private HrChartService hrChartService;

    @GetMapping("/employee-distribution")
    public Result<HrEmployeeChartVO> employeeDistribution() {
        return Result.success(hrChartService.employeeDistribution());
    }
}