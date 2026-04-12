package org.example.back.llm.service;

import org.example.back.config.ProjectAssistantProperties;
import org.example.back.llm.client.LlmChatClient;
import org.example.back.llm.dto.LlmInvocationContext;
import org.example.back.llm.dto.LlmChatRequest;
import org.example.back.llm.dto.LlmChatResponse;
import org.example.back.llm.dto.LlmModelOption;
import org.example.back.llm.exception.LlmBudgetExceededException;
import org.example.back.llm.enums.LlmProviderType;
import org.example.back.llm.factory.LlmProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class LlmRoutingService {

    private static final Logger log = LoggerFactory.getLogger(LlmRoutingService.class);

    private final ProjectAssistantProperties properties;
    private final LlmProviderRegistry providerRegistry;
    private final LlmGovernanceService governanceService;
    private final LlmAuditService auditService;

    public LlmRoutingService(ProjectAssistantProperties properties,
                             LlmProviderRegistry providerRegistry,
                             LlmGovernanceService governanceService,
                             LlmAuditService auditService) {
        this.properties = properties;
        this.providerRegistry = providerRegistry;
        this.governanceService = governanceService;
        this.auditService = auditService;
    }

    public boolean isProjectModelConfigured() {
        return !resolveProjectCandidates(null).isEmpty();
    }

    public boolean canUseGeneralQuery(String role) {
        if (!properties.getGeneral().isEnabled()) {
            return false;
        }

        if (!isProjectModelConfigured()) {
            return false;
        }

        return properties.getGeneral().getAllowedRoles().stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase())
                .anyMatch(value -> value.equals(role == null ? "" : role.trim().toLowerCase()));
    }

    public boolean canSelectModel(String role) {
        return properties.getLlm().getFrontSelection().getAllowedRoles().stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase())
                .anyMatch(value -> value.equals(role == null ? "" : role.trim().toLowerCase()));
    }

    public List<LlmModelOption> listSelectableModels(String role) {
        if (!canSelectModel(role)) {
            return List.of();
        }

        return resolveSelectableCandidates().stream()
                .map(candidate -> {
                    LlmModelOption option = new LlmModelOption();
                    option.setProviderCode(candidate.providerCode());
                    option.setModelCode(candidate.modelKey());
                    option.setDefaultSelected(candidate.modelKey().equals(properties.getLlm().getDefaultModel()));
                    return option;
                })
                .toList();
    }

    public LlmChatResponse chatWithProjectModel(String systemPrompt,
                                                String userPrompt,
                                                String requestedModelCode,
                                                LlmInvocationContext invocationContext) throws Exception {
        return chatWithCandidates(resolveProjectCandidates(requestedModelCode), systemPrompt, userPrompt, null, invocationContext);
    }

    public LlmChatResponse chatGeneralQuestion(String systemPrompt,
                                               String userPrompt,
                                               int maxTokens,
                                               String requestedModelCode,
                                               LlmInvocationContext invocationContext) throws Exception {
        return chatWithCandidates(resolveProjectCandidates(requestedModelCode), systemPrompt, userPrompt, maxTokens, invocationContext);
    }

    private LlmChatResponse chatWithCandidates(List<ResolvedModelCandidate> candidates,
                                               String systemPrompt,
                                               String userPrompt,
                                               Integer overrideMaxTokens,
                                               LlmInvocationContext invocationContext) throws Exception {
        if (candidates.isEmpty()) {
            throw new IllegalStateException("未找到可用的项目模型配置");
        }

        Exception lastException = null;
        boolean budgetBlocked = false;
        for (int i = 0; i < candidates.size(); i++) {
            ResolvedModelCandidate candidate = candidates.get(i);
            if (!governanceService.tryConsumeModelBudget(candidate.modelKey())) {
                budgetBlocked = true;
                auditService.record(invocationContext, null, candidate.providerCode(), candidate.modelKey(), i > 0, "budget_blocked", 0L);
                continue;
            }

            long startedAt = System.currentTimeMillis();
            try {
                LlmChatRequest request = new LlmChatRequest();
                request.setProviderType(candidate.providerType());
                request.setProviderCode(candidate.providerCode());
                request.setEndpoint(candidate.endpoint());
                request.setApiKey(candidate.apiKey());
                request.setModelCode(candidate.remoteModelCode());
                request.setTemperature(candidate.temperature());
                request.setMaxTokens(overrideMaxTokens != null ? overrideMaxTokens : candidate.maxTokens());
                request.setSystemPrompt(systemPrompt);
                request.setUserPrompt(userPrompt);

                LlmChatClient client = providerRegistry.getClient(request.getProviderType());
                LlmChatResponse response = client.chat(request);
                long latencyMs = System.currentTimeMillis() - startedAt;
                response.setFallbackUsed(i > 0);
                response.setLatencyMs(latencyMs);
                auditService.record(invocationContext, null, response.getProviderCode(), response.getModelCode(), response.isFallbackUsed(), "success", latencyMs);
                return response;
            } catch (Exception ex) {
                long latencyMs = System.currentTimeMillis() - startedAt;
                auditService.record(invocationContext, null, candidate.providerCode(), candidate.modelKey(), i > 0, "failed", latencyMs);
                lastException = ex;
                log.warn("LLM 模型调用失败，准备尝试下一个候选模型: {}", candidate.modelKey(), ex);
            }
        }

        if (budgetBlocked && lastException == null) {
            throw new LlmBudgetExceededException("当前模型预算已达上限，请稍后再试。");
        }

        throw lastException != null ? lastException : new IllegalStateException("所有候选模型均调用失败");
    }

    private List<ResolvedModelCandidate> resolveProjectCandidates(String requestedModelCode) {
        Set<String> candidateKeys = new LinkedHashSet<>();
        if (requestedModelCode != null && !requestedModelCode.isBlank()) {
            candidateKeys.add(requestedModelCode.trim());
        }
        String defaultModel = properties.getLlm().getDefaultModel();
        if (defaultModel != null && !defaultModel.isBlank()) {
            candidateKeys.add(defaultModel.trim());
        }
        candidateKeys.addAll(properties.getLlm().getFallbackChain());

        return resolveCandidates(candidateKeys);
    }

    private List<ResolvedModelCandidate> resolveSelectableCandidates() {
        return resolveCandidates(properties.getLlm().getModels().keySet());
    }

    private List<ResolvedModelCandidate> resolveCandidates(Iterable<String> candidateKeys) {

        List<ResolvedModelCandidate> resolved = new ArrayList<>();
        for (String modelKey : candidateKeys) {
            if (modelKey == null || modelKey.isBlank()) {
                continue;
            }
            ProjectAssistantProperties.ModelConfig config = properties.getLlm().getModels().get(modelKey.trim());
            if (config == null || !config.isEnabled()) {
                continue;
            }
            if (!isConfigured(config.getApiKey()) || !isConfigured(config.getEndpoint()) || !isConfigured(config.getModel())) {
                continue;
            }

            resolved.add(new ResolvedModelCandidate(
                    modelKey.trim(),
                    LlmProviderType.fromCode(config.getProvider()),
                    config.getProvider().trim().toLowerCase(),
                    config.getEndpoint().trim(),
                    config.getApiKey().trim(),
                    config.getModel().trim(),
                    config.getTemperature(),
                    config.getMaxTokens()
            ));
        }

        if (resolved.isEmpty() && isConfigured(properties.getDeepseek().getApiKey())) {
            ProjectAssistantProperties.DeepSeekConfig deepSeekConfig = properties.getDeepseek();
            resolved.add(new ResolvedModelCandidate(
                    "deepseek-legacy",
                    LlmProviderType.DEEPSEEK,
                    "deepseek",
                    deepSeekConfig.getBaseUrl().trim() + "/v1/chat/completions",
                    deepSeekConfig.getApiKey().trim(),
                    deepSeekConfig.getModel().trim(),
                    deepSeekConfig.getTemperature(),
                    deepSeekConfig.getMaxTokens()
            ));
        }

        return resolved;
    }

    private boolean isConfigured(String apiKey) {
        if (apiKey == null) {
            return false;
        }
        String normalized = apiKey.trim();
        return !normalized.isEmpty()
                && !normalized.startsWith("${")
                && !normalized.endsWith("}")
                && !"null".equalsIgnoreCase(normalized);
    }

    private record ResolvedModelCandidate(
            String modelKey,
            LlmProviderType providerType,
            String providerCode,
            String endpoint,
            String apiKey,
            String remoteModelCode,
            double temperature,
            int maxTokens
    ) {
    }
}