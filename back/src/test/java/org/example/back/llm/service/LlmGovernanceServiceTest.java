package org.example.back.llm.service;

import org.example.back.config.ProjectAssistantProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmGovernanceServiceTest {

    @Test
    void checkUserRateLimit_shouldBlockAfterThreshold() {
        ProjectAssistantProperties properties = new ProjectAssistantProperties();
        properties.getLlm().getRateLimit().setUserPerMinute(1);
        properties.getLlm().getRateLimit().setUserPerHour(0);
        LlmGovernanceService governanceService = new LlmGovernanceService(properties);

        assertTrue(governanceService.checkUserRateLimit(99L).isAllowed());
        LlmGovernanceService.RateLimitDecision blocked = governanceService.checkUserRateLimit(99L);

        assertFalse(blocked.isAllowed());
        assertTrue(blocked.getRetryAfterSeconds() >= 1L);
    }

    @Test
    void tryConsumeModelBudget_shouldBlockWhenDailyBudgetExceeded() {
        ProjectAssistantProperties properties = new ProjectAssistantProperties();
        ProjectAssistantProperties.BudgetConfig budgetConfig = new ProjectAssistantProperties.BudgetConfig();
        budgetConfig.setDailyRequests(1);
        properties.getLlm().getBudget().put("qwen-plus", budgetConfig);

        LlmGovernanceService governanceService = new LlmGovernanceService(properties);

        assertTrue(governanceService.tryConsumeModelBudget("qwen-plus"));
        assertFalse(governanceService.tryConsumeModelBudget("qwen-plus"));
    }
}