package org.example.back.llm.service;

import org.example.back.config.ProjectAssistantProperties;
import org.example.back.entity.AiModelCallLog;
import org.example.back.llm.dto.LlmInvocationContext;
import org.example.back.mapper.AiModelCallLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LlmAuditService {

    private final ProjectAssistantProperties properties;
    private final AiModelCallLogMapper aiModelCallLogMapper;

    public LlmAuditService(ProjectAssistantProperties properties, AiModelCallLogMapper aiModelCallLogMapper) {
        this.properties = properties;
        this.aiModelCallLogMapper = aiModelCallLogMapper;
    }

    public void record(LlmInvocationContext context,
                       Long assistantMessageId,
                       String providerCode,
                       String actualModelCode,
                       Boolean fallbackUsed,
                       String resultStatus,
                       Long latencyMs) {
        if (!properties.getLlm().getAudit().isEnabled()) {
            return;
        }

        AiModelCallLog auditLog = new AiModelCallLog();
        auditLog.setUserId(context == null ? null : context.getUserId());
        auditLog.setRoleCode(normalize(context == null ? null : context.getRoleCode(), 20));
        auditLog.setDeptCode(normalize(context == null ? null : context.getDeptCode(), 32));
        auditLog.setConversationId(context == null ? null : context.getConversationId());
        auditLog.setAssistantMessageId(assistantMessageId);
        auditLog.setSceneCode(normalize(context == null ? null : context.getSceneCode(), 32));
        auditLog.setQuestionType(normalize(context == null ? null : context.getQuestionType(), 20));
        auditLog.setRequestedModelCode(normalize(context == null ? null : context.getRequestedModelCode(), 64));
        auditLog.setProviderCode(normalize(providerCode, 32));
        auditLog.setModelCode(normalize(actualModelCode, 64));
        auditLog.setFallbackUsed(Boolean.TRUE.equals(fallbackUsed));
        auditLog.setHitType(normalize(context == null ? null : context.getHitType(), 30));
        auditLog.setResultStatus(normalize(resultStatus, 20));
        auditLog.setLatencyMs(latencyMs);
        auditLog.setQuestionExcerpt(normalize(context == null ? null : context.getQuestionExcerpt(), 200));
        auditLog.setCreatedAt(LocalDateTime.now());
        aiModelCallLogMapper.insert(auditLog);
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
}