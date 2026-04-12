package org.example.back.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.entity.AiConversation;
import org.example.back.entity.AiMessage;
import org.example.back.mapper.AiConversationMapper;
import org.example.back.mapper.AiMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiConversationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiMessageMapper messageMapper;

    /**
     * 创建新会话
     */
    public AiConversation createConversation(Long userId, String title) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setTitle(title != null && title.length() > 100 ? title.substring(0, 100) : title);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(conv);
        return conv;
    }

    /**
     * 查询用户最近30天的会话列表
     */
    public Page<AiConversation> listConversations(Long userId, int pageNum, int pageSize) {
        Page<AiConversation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
               .ge(AiConversation::getCreatedAt, LocalDateTime.now().minusDays(30))
               .orderByDesc(AiConversation::getUpdatedAt);
        return conversationMapper.selectPage(page, wrapper);
    }

    /**
     * 获取某会话的全部消息（校验归属）
     */
    public List<AiMessage> getConversationMessages(Long conversationId, Long userId) {
        // 先校验会话归属
        AiConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            return List.of();
        }

        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getConversationId, conversationId)
               .orderByAsc(AiMessage::getCreatedAt);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 删除会话及其消息
     */
    @Transactional
    public boolean deleteConversation(Long conversationId, Long userId) {
        AiConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            return false;
        }

        // 先删消息
        LambdaQueryWrapper<AiMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(AiMessage::getConversationId, conversationId);
        messageMapper.delete(msgWrapper);

        // 再删会话
        conversationMapper.deleteById(conversationId);
        return true;
    }

    /**
     * 清空某个用户的全部会话及消息
     */
    @Transactional
    public void clearAllConversations(Long userId) {
        LambdaQueryWrapper<AiConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(AiConversation::getUserId, userId);
        List<AiConversation> conversations = conversationMapper.selectList(convWrapper);

        if (conversations.isEmpty()) {
            return;
        }

        List<Long> conversationIds = conversations.stream().map(AiConversation::getId).toList();

        LambdaQueryWrapper<AiMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.in(AiMessage::getConversationId, conversationIds);
        messageMapper.delete(msgWrapper);

        conversationMapper.delete(convWrapper);
    }

    /**
     * 成对保存 user + assistant 消息
     */
    @Transactional
    public void saveMessagePair(Long conversationId, Long userId,
                                String userContent, String assistantContent,
                                String sourcesJson, String hitType,
                                String providerCode, String modelCode, Boolean fallbackUsed) {
        // 校验归属
        AiConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new IllegalArgumentException("会话不存在或无权操作");
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedSourcesJson = normalizeSourcesJson(sourcesJson);
        String normalizedHitType = normalizeTextField(hitType, 30);
        String normalizedProviderCode = normalizeTextField(providerCode, 32);
        String normalizedModelCode = normalizeTextField(modelCode, 64);
        Boolean normalizedFallbackUsed = hasModelMetadata(normalizedProviderCode, normalizedModelCode)
                ? Boolean.TRUE.equals(fallbackUsed)
                : null;

        // 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(userContent);
        userMsg.setCreatedAt(now);
        messageMapper.insert(userMsg);

        // 保存助手消息
        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(assistantContent);
        assistantMsg.setSourcesJson(normalizedSourcesJson);
        assistantMsg.setHitType(normalizedHitType);
        assistantMsg.setProviderCode(normalizedProviderCode);
        assistantMsg.setModelCode(normalizedModelCode);
        assistantMsg.setFallbackUsed(normalizedFallbackUsed);
        assistantMsg.setCreatedAt(now.plusNanos(1000));
        messageMapper.insert(assistantMsg);

        // 更新会话时间
        conv.setUpdatedAt(now);
        conversationMapper.updateById(conv);
    }

    private boolean hasModelMetadata(String providerCode, String modelCode) {
        return providerCode != null && !providerCode.isBlank()
                && modelCode != null && !modelCode.isBlank();
    }

    private String normalizeTextField(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String normalizeSourcesJson(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(sourcesJson);
            if (!root.isArray()) {
                throw new IllegalArgumentException("来源文档格式不正确");
            }
            return objectMapper.writeValueAsString(root);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("来源文档格式不正确");
        }
    }

    /**
     * 清理30天前的会话和消息
     */
    @Transactional
    public int cleanupExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        // 查出过期会话ID
        LambdaQueryWrapper<AiConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.lt(AiConversation::getCreatedAt, cutoff);
        List<AiConversation> expired = conversationMapper.selectList(convWrapper);

        if (expired.isEmpty()) {
            return 0;
        }

        List<Long> expiredIds = expired.stream().map(AiConversation::getId).toList();

        // 删消息
        LambdaQueryWrapper<AiMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.in(AiMessage::getConversationId, expiredIds);
        messageMapper.delete(msgWrapper);

        // 删会话
        conversationMapper.delete(convWrapper);

        return expiredIds.size();
    }
}
