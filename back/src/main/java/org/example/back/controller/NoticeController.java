package org.example.back.controller;

import jakarta.validation.Valid;
import org.example.back.common.annotation.PreventDuplicateSubmit;
import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.NoticeQueryDTO;
import org.example.back.dto.NoticeSaveDTO;
import org.example.back.service.NoticeService;
import org.example.back.vo.NoticeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping("/page")
    public Result<PageResult<NoticeVO>> page(NoticeQueryDTO queryDTO) {
        return Result.success(noticeService.page(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<NoticeVO> getById(@PathVariable Long id) {
        return Result.success(noticeService.getById(id));
    }

    @GetMapping("/home/latest")
    public Result<List<NoticeVO>> homeLatest(@RequestParam(defaultValue = "4") Integer limit) {
        return Result.success(noticeService.listAdminHomeLatest(limit == null ? 4 : limit));
    }

    @PostMapping
    @RequireAdmin("仅管理员可新增公告")
    @PreventDuplicateSubmit(message = "请勿重复提交公告新增请求")
    public Result<Void> create(@Valid @RequestBody NoticeSaveDTO dto) {
        noticeService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequireAdmin("仅管理员可编辑公告")
    @PreventDuplicateSubmit(message = "请勿重复提交公告编辑请求")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeSaveDTO dto) {
        noticeService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin("仅管理员可删除公告")
    @PreventDuplicateSubmit(intervalMs = 1000, message = "删除请求过于频繁，请稍后再试")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.success();
    }
}