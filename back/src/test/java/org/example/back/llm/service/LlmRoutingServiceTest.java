package org.example.back.llm.service;

import org.example.back.config.ProjectAssistantProperties;
import org.example.back.llm.client.LlmChatClient;
import org.example.back.llm.dto.LlmChatResponse;
import org.example.back.llm.dto.LlmInvocationContext;
import org.example.back.llm.dto.LlmModelOption;
import org.example.back.llm.enums.LlmProviderType;
import org.example.back.llm.factory.LlmProviderRegistry;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmRoutingServiceTest {

    @Test
    void listSelectableModels_shouldIncludeKimiWhenConfigured() {
        ProjectAssistantProperties properties = new ProjectAssistantProperties();
        properties.getLlm().setDefaultModel("qwen-plus");

        ProjectAssistantProperties.ModelConfig qwen = new ProjectAssistantProperties.ModelConfig();
        qwen.setProvider("qwen");
        qwen.setEndpoint("https://example.com/qwen");
        qwen.setApiKey("qwen-key");
        qwen.setModel("qwen-plus");

        ProjectAssistantProperties.ModelConfig glm = new ProjectAssistantProperties.ModelConfig();
        glm.setProvider("glm");
        glm.setEndpoint("https://example.com/glm");
        glm.setApiKey("glm-key");
        glm.setModel("glm-4-flash");

        ProjectAssistantProperties.ModelConfig kimi = new ProjectAssistantProperties.ModelConfig();
        kimi.setProvider("kimi");
        kimi.setEndpoint("https://example.com/kimi");
        kimi.setApiKey("kimi-key");
        kimi.setModel("moonshot-v1-8k");

        properties.getLlm().setModels(new LinkedHashMap<>());
        properties.getLlm().getModels().put("qwen-plus", qwen);
        properties.getLlm().getModels().put("glm-4-flash", glm);
        properties.getLlm().getModels().put("kimi-8k", kimi);

        LlmProviderRegistry providerRegistry = mock(LlmProviderRegistry.class);
        LlmGovernanceService governanceService = mock(LlmGovernanceService.class);
        LlmAuditService auditService = mock(LlmAuditService.class);

        LlmRoutingService routingService = new LlmRoutingService(properties, providerRegistry, governanceService, auditService);

        List<LlmModelOption> options = routingService.listSelectableModels("admin");

        assertEquals(3, options.size());
        assertIterableEquals(
            List.of("qwen-plus", "glm-4-flash", "kimi-8k"),
                options.stream().map(LlmModelOption::getModelCode).toList()
        );
        assertEquals("kimi", options.get(2).getProviderCode());
        assertTrue(options.get(0).isDefaultSelected());
    }

    @Test
    void chatWithProjectModel_shouldFallbackAfterBudgetBlockedCandidate() throws Exception {
        ProjectAssistantProperties properties = new ProjectAssistantProperties();
        properties.getLlm().setDefaultModel("qwen-plus");
        properties.getLlm().setFallbackChain(List.of("glm-4-flash"));

        ProjectAssistantProperties.ModelConfig qwen = new ProjectAssistantProperties.ModelConfig();
        qwen.setProvider("qwen");
        qwen.setEndpoint("https://example.com/qwen");
        qwen.setApiKey("qwen-key");
        qwen.setModel("qwen-plus");

        ProjectAssistantProperties.ModelConfig glm = new ProjectAssistantProperties.ModelConfig();
        glm.setProvider("glm");
        glm.setEndpoint("https://example.com/glm");
        glm.setApiKey("glm-key");
        glm.setModel("glm-4-flash");

        properties.getLlm().setModels(new LinkedHashMap<>());
        properties.getLlm().getModels().put("qwen-plus", qwen);
        properties.getLlm().getModels().put("glm-4-flash", glm);

        LlmProviderRegistry providerRegistry = mock(LlmProviderRegistry.class);
        LlmChatClient chatClient = mock(LlmChatClient.class);
        LlmGovernanceService governanceService = mock(LlmGovernanceService.class);
        LlmAuditService auditService = mock(LlmAuditService.class);

        when(providerRegistry.getClient(LlmProviderType.GLM)).thenReturn(chatClient);
        when(governanceService.tryConsumeModelBudget("qwen-plus")).thenReturn(false);
        when(governanceService.tryConsumeModelBudget("glm-4-flash")).thenReturn(true);

        LlmChatResponse response = new LlmChatResponse();
        response.setContent("fallback answer");
        response.setProviderCode("glm");
        response.setModelCode("glm-4-flash");
        when(chatClient.chat(any())).thenReturn(response);

        LlmRoutingService routingService = new LlmRoutingService(properties, providerRegistry, governanceService, auditService);
        LlmInvocationContext context = new LlmInvocationContext();
        context.setSceneCode("project-assistant");
        context.setQuestionType("project");

        LlmChatResponse actual = routingService.chatWithProjectModel("system", "question", "qwen-plus", context);

        assertEquals("glm-4-flash", actual.getModelCode());
        assertTrue(actual.isFallbackUsed());
        verify(auditService).record(eq(context), eq(null), eq("qwen"), eq("qwen-plus"), eq(false), eq("budget_blocked"), eq(0L));
        verify(auditService).record(eq(context), eq(null), eq("glm"), eq("glm-4-flash"), eq(true), eq("success"), anyLong());
    }
}