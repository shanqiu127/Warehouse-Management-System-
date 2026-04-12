package org.example.back.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.example.back.common.AssistantAnswerMode;
import org.example.back.config.ProjectAssistantProperties;
import org.example.back.llm.dto.LlmChatResponse;
import org.example.back.llm.dto.LlmInvocationContext;
import org.example.back.llm.dto.LlmModelOption;
import org.example.back.llm.exception.LlmBudgetExceededException;
import org.example.back.llm.service.LlmAuditService;
import org.example.back.llm.service.LlmGovernanceService;
import org.example.back.llm.service.LlmRoutingService;
import org.example.back.vo.ProjectAssistantModelOptionVO;
import org.example.back.vo.ProjectAssistantAnswerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAssistantServiceTest {

    @Mock
    private ProjectKnowledgeBaseService knowledgeBaseService;

    @Mock
    private ProjectKnowledgeRetrievalService retrievalService;

    @Mock
    private LlmRoutingService llmRoutingService;

    @Mock
    private LlmGovernanceService llmGovernanceService;

    @Mock
    private LlmAuditService llmAuditService;

    @InjectMocks
    private ProjectAssistantService projectAssistantService;

    @BeforeEach
    void setUp() {
        ProjectAssistantProperties properties = new ProjectAssistantProperties();
        properties.getDeepseek().setApiKey("");
        ReflectionTestUtils.setField(projectAssistantService, "properties", properties);
    }

    @Test
    void query_shouldUseKnowledgeBaseMissFlowForProjectQuestionWithoutWhitelistKeyword() {
        String question = "登录失败怎么排查";
        SaSession session = mock(SaSession.class);

        when(retrievalService.retrieve(question, "employee", null)).thenReturn(List.of());
        when(retrievalService.isBelowThreshold(List.of())).thenReturn(true);
        when(retrievalService.isLikelyProjectQuestion(question)).thenReturn(true);
        when(session.get("role")).thenReturn("employee");
        when(session.get("deptCode")).thenReturn(null);

        ProjectAssistantAnswerVO answer;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getSession).thenReturn(session);
            when(llmRoutingService.isProjectModelConfigured()).thenReturn(false);
            answer = projectAssistantService.query(question, null, null, null);
        }

        assertNotNull(answer);
        assertTrue(answer.getAnswer().contains("抱歉，我没有找到相关的知识库"));
        assertTrue("no-hit".equals(answer.getHitType()));
        assertTrue(AssistantAnswerMode.HYBRID.getCode().equals(answer.getMode()));
    }

    @Test
    void query_shouldRejectWhenStrictModeMissesKnowledgeBase() {
        String question = "登录失败怎么排查";
        SaSession session = mock(SaSession.class);

        when(retrievalService.retrieve(question, "employee", null)).thenReturn(List.of());
        when(retrievalService.isBelowThreshold(List.of())).thenReturn(true);
        when(retrievalService.isLikelyProjectQuestion(question)).thenReturn(true);
        when(session.get("role")).thenReturn("employee");
        when(session.get("deptCode")).thenReturn(null);

        ProjectAssistantAnswerVO answer;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getSession).thenReturn(session);
            answer = projectAssistantService.query(question, AssistantAnswerMode.STRICT.getCode(), null, null);
        }

        assertNotNull(answer);
        assertTrue(answer.getAnswer().contains("仅按项目文档回答模式下"));
        assertTrue("kb-miss-strict".equals(answer.getHitType()));
        assertTrue(AssistantAnswerMode.STRICT.getCode().equals(answer.getMode()));
    }

    @Test
    void buildSystemPrompt_shouldContainStrictModeInstruction() {
        String prompt = ReflectionTestUtils.invokeMethod(
                projectAssistantService,
                "buildSystemPrompt",
                "employee",
                null,
                "普通员工",
                AssistantAnswerMode.STRICT
        );

        assertNotNull(prompt);
        assertFalse(prompt.contains("当前文档未提供充分依据"));
        assertTrue(prompt.contains("当前模式为仅按项目文档回答"));
        assertTrue(prompt.contains("优先给出可执行步骤"));
        assertTrue(prompt.contains("不要编造不存在的细节"));
    }

    @Test
    void query_shouldUseRequestedModelForAdminWhenKnowledgeBaseMissFallsBackToModel() throws Exception {
        String question = "这个问题知识库里没有";
        SaSession session = mock(SaSession.class);
        LlmChatResponse response = new LlmChatResponse();
        response.setContent("模型回答");
        response.setProviderCode("glm");
        response.setModelCode("glm-4-flash");

        when(retrievalService.retrieve(question, "admin", "warehouse")).thenReturn(List.of());
        when(retrievalService.isBelowThreshold(List.of())).thenReturn(true);
        when(retrievalService.isLikelyProjectQuestion(question)).thenReturn(true);
        when(session.get("role")).thenReturn("admin");
        when(session.get("deptCode")).thenReturn("warehouse");
        when(llmRoutingService.isProjectModelConfigured()).thenReturn(true);
        when(llmRoutingService.canSelectModel("admin")).thenReturn(true);
        when(llmGovernanceService.checkUserRateLimit(anyLong())).thenReturn(LlmGovernanceService.RateLimitDecision.allowed());
        when(llmRoutingService.chatWithProjectModel(anyString(), eq(question), eq("glm-4-flash"), any(LlmInvocationContext.class))).thenReturn(response);

        ProjectAssistantAnswerVO answer;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getSession).thenReturn(session);
            answer = projectAssistantService.query(question, null, "glm-4-flash", 1L);
        }

        assertEquals("glm-4-flash", answer.getModelCode());
        verify(llmRoutingService).chatWithProjectModel(anyString(), eq(question), eq("glm-4-flash"), any(LlmInvocationContext.class));
    }

    @Test
    void query_shouldIgnoreRequestedModelForEmployee() throws Exception {
        String question = "这个问题知识库里没有";
        SaSession session = mock(SaSession.class);
        LlmChatResponse response = new LlmChatResponse();
        response.setContent("模型回答");

        when(retrievalService.retrieve(question, "employee", null)).thenReturn(List.of());
        when(retrievalService.isBelowThreshold(List.of())).thenReturn(true);
        when(retrievalService.isLikelyProjectQuestion(question)).thenReturn(true);
        when(session.get("role")).thenReturn("employee");
        when(session.get("deptCode")).thenReturn(null);
        when(llmRoutingService.isProjectModelConfigured()).thenReturn(true);
        when(llmRoutingService.canSelectModel("employee")).thenReturn(false);
        when(llmGovernanceService.checkUserRateLimit(anyLong())).thenReturn(LlmGovernanceService.RateLimitDecision.allowed());
        when(llmRoutingService.chatWithProjectModel(anyString(), eq(question), isNull(), any(LlmInvocationContext.class))).thenReturn(response);

        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getSession).thenReturn(session);
            projectAssistantService.query(question, null, "glm-4-flash", 1L);
        }

        verify(llmRoutingService).chatWithProjectModel(anyString(), eq(question), isNull(), any(LlmInvocationContext.class));
    }

    @Test
    void listAvailableModels_shouldReturnSelectableModelsForAdmin() {
        SaSession session = mock(SaSession.class);
        LlmModelOption qwen = new LlmModelOption();
        qwen.setProviderCode("qwen");
        qwen.setModelCode("qwen-plus");
        qwen.setDefaultSelected(true);

        when(session.get("role")).thenReturn("admin");
        when(llmRoutingService.listSelectableModels("admin")).thenReturn(List.of(qwen));

        List<ProjectAssistantModelOptionVO> models;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getSession).thenReturn(session);
            models = projectAssistantService.listAvailableModels();
        }

        assertEquals(1, models.size());
        assertEquals("千问 Plus", models.get(0).getLabel());
        assertTrue(models.get(0).isDefaultSelected());
    }

    @Test
    void listAvailableModels_shouldReturnEmptyForEmployee() {
        SaSession session = mock(SaSession.class);

        when(session.get("role")).thenReturn("employee");
        when(llmRoutingService.listSelectableModels("employee")).thenReturn(List.of());

        List<ProjectAssistantModelOptionVO> models;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getSession).thenReturn(session);
            models = projectAssistantService.listAvailableModels();
        }

        assertTrue(models.isEmpty());
    }

    @Test
    void query_shouldReturnRateLimitedAnswerWhenKnowledgeBaseMissTriggersGovernance() {
        String question = "这个问题知识库里没有";
        SaSession session = mock(SaSession.class);

        when(retrievalService.retrieve(question, "admin", "warehouse")).thenReturn(List.of());
        when(retrievalService.isBelowThreshold(List.of())).thenReturn(true);
        when(retrievalService.isLikelyProjectQuestion(question)).thenReturn(true);
        when(session.get("role")).thenReturn("admin");
        when(session.get("deptCode")).thenReturn("warehouse");
        when(llmRoutingService.isProjectModelConfigured()).thenReturn(true);
        when(llmRoutingService.canSelectModel("admin")).thenReturn(true);
        when(llmGovernanceService.checkUserRateLimit(anyLong())).thenReturn(LlmGovernanceService.RateLimitDecision.blocked(25L));

        ProjectAssistantAnswerVO answer;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getSession).thenReturn(session);
            answer = projectAssistantService.query(question, null, "qwen-plus", 1L);
        }

        assertEquals("rate-limited", answer.getHitType());
        assertTrue(answer.getAnswer().contains("25 秒后再试"));
    }

    @Test
    void query_shouldReturnBudgetBlockedAnswerForGeneralQuestionWhenAllModelsBlocked() throws Exception {
        String question = "帮我解释一下完全无关项目的问题";
        SaSession session = mock(SaSession.class);

        when(retrievalService.retrieve(question, "admin", "warehouse")).thenReturn(List.of());
        when(retrievalService.isBelowThreshold(List.of())).thenReturn(true);
        when(retrievalService.isLikelyProjectQuestion(question)).thenReturn(false);
        when(session.get("role")).thenReturn("admin");
        when(session.get("deptCode")).thenReturn("warehouse");
        when(llmRoutingService.canSelectModel("admin")).thenReturn(true);
        when(llmRoutingService.canUseGeneralQuery("admin")).thenReturn(true);
        when(llmGovernanceService.checkUserRateLimit(anyLong())).thenReturn(LlmGovernanceService.RateLimitDecision.allowed());
        when(llmRoutingService.chatGeneralQuestion(anyString(), eq(question), anyInt(), eq("qwen-plus"), any(LlmInvocationContext.class)))
                .thenThrow(new LlmBudgetExceededException("budget"));

        ProjectAssistantAnswerVO answer;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getSession).thenReturn(session);
            answer = projectAssistantService.query(question, null, "qwen-plus", 1L);
        }

        assertEquals("budget-blocked", answer.getHitType());
        assertTrue(answer.getAnswer().contains("千问 Plus 今日预算已达上限"));
    }
}