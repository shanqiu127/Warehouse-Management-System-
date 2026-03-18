package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.EmployeeQueryDTO;
import org.example.back.dto.EmployeeSaveDTO;
import org.example.back.service.EmployeeService;
import org.example.back.vo.EmployeeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/employees")
@RequireAdmin("仅管理员可访问员工管理")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/page")
    public Result<PageResult<EmployeeVO>> page(EmployeeQueryDTO queryDTO) {
        return Result.success(employeeService.page(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<EmployeeVO> getById(@PathVariable Long id) {
        return Result.success(employeeService.getById(id));
    }

    @PostMapping
    @PreventDuplicateSubmit(message = "请勿重复提交员工新增请求")
    public Result<Void> create(@Valid @RequestBody EmployeeSaveDTO dto) {
        employeeService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreventDuplicateSubmit(message = "请勿重复提交员工编辑请求")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EmployeeSaveDTO dto) {
        employeeService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.success();
    }
}