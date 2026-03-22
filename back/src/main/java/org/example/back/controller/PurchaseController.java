package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.PurchaseQueryDTO;
import org.example.back.dto.PurchaseSaveDTO;
import org.example.back.dto.DocumentVoidDTO;
import org.example.back.service.PurchaseService;
import org.example.back.vo.PurchaseSourceOptionVO;
import org.example.back.vo.PurchaseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @GetMapping("/page")
    public Result<PageResult<PurchaseVO>> page(PurchaseQueryDTO queryDTO) {
        return Result.success(purchaseService.page(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<PurchaseVO> getById(@PathVariable Long id) {
        return Result.success(purchaseService.getById(id));
    }

    @GetMapping("/options/returnable")
    public Result<List<PurchaseSourceOptionVO>> returnableOptions(@RequestParam(required = false) Long goodsId) {
        return Result.success(purchaseService.returnableOptions(goodsId));
    }

    @PostMapping
    @RequireAdmin("仅管理员可新增进货单")
    @PreventDuplicateSubmit(intervalMs = 1800, message = "请勿重复提交进货单")
    public Result<Void> create(@Valid @RequestBody PurchaseSaveDTO dto) {
        purchaseService.create(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin("仅管理员可删除进货单")
    @PreventDuplicateSubmit(intervalMs = 1200, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        purchaseService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/void")
    @RequireAdmin("仅管理员可作废进货单")
    @PreventDuplicateSubmit(intervalMs = 1500, message = "请勿重复提交作废请求")
    public Result<Void> voidDocument(@PathVariable Long id, @RequestBody(required = false) DocumentVoidDTO dto) {
        purchaseService.voidDocument(id, dto);
        return Result.success();
    }
}
