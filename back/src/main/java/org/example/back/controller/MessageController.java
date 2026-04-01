package org.example.back.controller;

import org.example.back.common.annotation.RequireAdmin;
import org.example.back.common.result.PageResult;
import org.example.back.common.result.Result;
import org.example.back.dto.MessageQueryDTO;
import org.example.back.service.MessageService;
import org.example.back.vo.MessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/messages")
@RequireAdmin("仅管理员可访问消息中心")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/page")
    public Result<PageResult<MessageVO>> page(MessageQueryDTO queryDTO) {
        return Result.success(messageService.page(queryDTO));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(messageService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead();
        return Result.success();
    }

    @DeleteMapping("/read")
    public Result<Void> deleteAllRead() {
        messageService.deleteAllRead();
        return Result.success();
    }
}