package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.AuditLog;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.SalesQueryDTO;
import org.example.back.dto.SalesSaveDTO;
import org.example.back.dto.DocumentVoidDTO;
import org.example.back.service.SalesService;
import org.example.back.vo.SalesSourceOptionVO;
import org.example.back.vo.SalesVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business/sales")
public class SalesController {

    @Autowired
    private SalesService salesService;

    @GetMapping("/page")
    public Result<PageResult<SalesVO>> page(SalesQueryDTO queryDTO) {
        return Result.success(salesService.page(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<SalesVO> getById(@PathVariable Long id) {
        return Result.success(salesService.getById(id));
    }

    @GetMapping("/options/returnable")
    public Result<List<SalesSourceOptionVO>> returnableOptions(@RequestParam(required = false) Long goodsId) {
        return Result.success(salesService.returnableOptions(goodsId));
    }

    @PostMapping
    @PreventDuplicateSubmit(intervalMs = 1800, message = "请勿重复提交销售单")
    public Result<Void> create(@Valid @RequestBody SalesSaveDTO dto) {
        salesService.create(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "销售管理", action = "删除", targetType = "销售单")
    @PreventDuplicateSubmit(intervalMs = 1200, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        salesService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/void")
    @AuditLog(module = "销售管理", action = "作废/作废并红冲", targetType = "销售单")
    @RequireAdmin("仅管理员可作废销售单")
    @PreventDuplicateSubmit(intervalMs = 1500, message = "请勿重复提交作废请求")
    public Result<Void> voidDocument(@PathVariable Long id, @RequestBody(required = false) DocumentVoidDTO dto) {
        salesService.voidDocument(id, dto);
        return Result.success();
    }
}
