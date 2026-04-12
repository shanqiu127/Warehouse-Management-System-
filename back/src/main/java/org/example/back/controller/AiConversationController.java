package org.example.back.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.back.common.result.Result;
import org.example.back.dto.AiMessagePairDTO;
import org.example.back.entity.AiConversation;
import org.example.back.entity.AiMessage;
import org.example.back.service.AiConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/assistant/conversation")
public class AiConversationController {

    @Autowired
    private AiConversationService conversationService;

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 创建新会话
     */
    @PostMapping
    public Result<AiConversation> create(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "新对话");
        AiConversation conv = conversationService.createConversation(getCurrentUserId(), title);
        return Result.success(conv);
    }

    /**
     * 分页查询会话列表
     */
    @GetMapping("/list")
    public Result<Page<AiConversation>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<AiConversation> page = conversationService.listConversations(getCurrentUserId(), pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取会话消息
     */
    @GetMapping("/{id}/messages")
    public Result<List<AiMessage>> messages(@PathVariable Long id) {
        List<AiMessage> messages = conversationService.getConversationMessages(id, getCurrentUserId());
        return Result.success(messages);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = conversationService.deleteConversation(id, getCurrentUserId());
        return ok ? Result.success() : Result.fail("会话不存在或无权删除");
    }

    /**
     * 清空当前用户全部会话历史
     */
    @DeleteMapping("/all")
    public Result<Void> clearAll() {
        conversationService.clearAllConversations(getCurrentUserId());
        return Result.success();
    }

    /**
     * 成对保存消息（user + assistant）
     */
    @PostMapping("/{id}/messages")
    public Result<Void> saveMessages(@PathVariable Long id, @Valid @RequestBody AiMessagePairDTO dto) {
        conversationService.saveMessagePair(
                id, getCurrentUserId(),
                dto.getUserContent(), dto.getAssistantContent(),
                dto.getSourcesJson(), dto.getHitType(),
                dto.getProviderCode(), dto.getModelCode(), dto.getFallbackUsed()
        );
        return Result.success();
    }
}
