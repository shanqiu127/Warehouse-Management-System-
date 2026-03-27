package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.AuditLog;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.SalesReturnQueryDTO;
import org.example.back.dto.SalesReturnSaveDTO;
import org.example.back.dto.DocumentVoidDTO;
import org.example.back.service.SalesReturnService;
import org.example.back.vo.SalesReturnVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/sales-returns")
public class SalesReturnController {

    @Autowired
    private SalesReturnService salesReturnService;

    @GetMapping("/page")
    public Result<PageResult<SalesReturnVO>> page(SalesReturnQueryDTO queryDTO) {
        return Result.success(salesReturnService.page(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<SalesReturnVO> getById(@PathVariable Long id) {
        return Result.success(salesReturnService.getById(id));
    }

    @PostMapping
    @PreventDuplicateSubmit(intervalMs = 1800, message = "请勿重复提交销售退货单")
    public Result<Void> create(@Valid @RequestBody SalesReturnSaveDTO dto) {
        salesReturnService.create(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "销售退货管理", action = "删除", targetType = "销售退货单")
    @PreventDuplicateSubmit(intervalMs = 1200, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        salesReturnService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/void")
    @AuditLog(module = "销售退货管理", action = "作废/作废并红冲", targetType = "销售退货单")
    @RequireAdmin("仅管理员可作废客退单")
    @PreventDuplicateSubmit(intervalMs = 1500, message = "请勿重复提交作废请求")
    public Result<Void> voidDocument(@PathVariable Long id, @RequestBody(required = false) DocumentVoidDTO dto) {
        salesReturnService.voidDocument(id, dto);
        return Result.success();
    }
}
