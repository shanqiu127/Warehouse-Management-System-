package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.PurchaseReturnQueryDTO;
import org.example.back.dto.PurchaseReturnSaveDTO;
import org.example.back.dto.DocumentVoidDTO;
import org.example.back.service.PurchaseReturnService;
import org.example.back.vo.PurchaseReturnVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/business/purchase-returns")
public class PurchaseReturnController {

    @Autowired
    private PurchaseReturnService purchaseReturnService;

    @GetMapping("/page")
    public Result<PageResult<PurchaseReturnVO>> page(PurchaseReturnQueryDTO queryDTO) {
        return Result.success(purchaseReturnService.page(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<PurchaseReturnVO> getById(@PathVariable Long id) {
        return Result.success(purchaseReturnService.getById(id));
    }

    @PostMapping
    @RequireAdmin("仅管理员可新增进货退货单")
    @PreventDuplicateSubmit(intervalMs = 1800, message = "请勿重复提交进货退货单")
    public Result<Void> create(@Valid @RequestBody PurchaseReturnSaveDTO dto) {
        purchaseReturnService.create(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin("仅管理员可删除进货退货单")
    @PreventDuplicateSubmit(intervalMs = 1200, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        purchaseReturnService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/void")
    @RequireAdmin("仅管理员可作废进货退货单")
    @PreventDuplicateSubmit(intervalMs = 1500, message = "请勿重复提交作废请求")
    public Result<Void> voidDocument(@PathVariable Long id, @RequestBody(required = false) DocumentVoidDTO dto) {
        purchaseReturnService.voidDocument(id, dto);
        return Result.success();
    }
}
