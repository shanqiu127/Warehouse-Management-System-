package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.SupplierQueryDTO;
import org.example.back.dto.SupplierSaveDTO;
import org.example.back.service.SupplierService;
import org.example.back.vo.OptionVO;
import org.example.back.vo.SupplierVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/base/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping("/page")
    public Result<PageResult<SupplierVO>> page(SupplierQueryDTO queryDTO) {
        return Result.success(supplierService.page(queryDTO));
    }

    @GetMapping("/options")
    public Result<List<OptionVO>> options() {
        return Result.success(supplierService.options());
    }

    @GetMapping("/{id}")
    public Result<SupplierVO> getById(@PathVariable Long id) {
        return Result.success(supplierService.getById(id));
    }

    @PostMapping
    @PreventDuplicateSubmit(message = "请勿重复提交供应商新增请求")
    public Result<Void> create(@Valid @RequestBody SupplierSaveDTO dto) {
        supplierService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreventDuplicateSubmit(message = "请勿重复提交供应商编辑请求")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SupplierSaveDTO dto) {
        supplierService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return Result.success();
    }
}