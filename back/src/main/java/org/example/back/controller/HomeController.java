package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.result.Result;
import org.example.back.dto.EmployeeContactUpdateDTO;
import org.example.back.service.HomeService;
import org.example.back.vo.EmployeeWorkbenchVO;
import org.example.back.vo.HomeSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    @GetMapping("/summary")
    public Result<HomeSummaryVO> summary() {
        return Result.success(homeService.summary());
    }

    @GetMapping("/employee-workbench")
    public Result<EmployeeWorkbenchVO> employeeWorkbench() {
        return Result.success(homeService.employeeWorkbench());
    }

    @PutMapping("/employee-workbench/contact")
    public Result<Void> updateEmployeeContact(@Valid @RequestBody EmployeeContactUpdateDTO dto) {
        homeService.updateEmployeeContact(dto);
        return Result.success();
    }
}
