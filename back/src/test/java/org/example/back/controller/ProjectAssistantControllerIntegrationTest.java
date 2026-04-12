package org.example.back.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.example.back.common.exception.GlobalExceptionHandler;
import org.example.back.config.ProjectAssistantProperties;
import org.example.back.config.SaTokenConfig;
import org.example.back.config.StpInterfaceImpl;
import org.example.back.entity.AiConversation;
import org.example.back.entity.AiMessage;
import org.example.back.llm.client.LlmChatClient;
import org.example.back.llm.dto.LlmChatRequest;
import org.example.back.llm.dto.LlmChatResponse;
import org.example.back.llm.enums.LlmProviderType;
import org.example.back.llm.factory.LlmProviderRegistry;
import org.example.back.llm.service.LlmAuditService;
import org.example.back.llm.service.LlmGovernanceService;
import org.example.back.llm.service.LlmRoutingService;
import org.example.back.mapper.AiConversationMapper;
import org.example.back.mapper.AiMessageMapper;
import org.example.back.mapper.SysErrorLogMapper;
import org.example.back.service.AiConversationService;
import org.example.back.service.ProjectAssistantService;
import org.example.back.service.ProjectKnowledgeBaseService;
import org.example.back.service.ProjectKnowledgeRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ProjectAssistantControllerIntegrationTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:assistant_integration;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "assistant.project.enabled=false",
                "assistant.project.llm.default-model=qwen-plus",
                "assistant.project.llm.fallback-chain=glm-4-flash,kimi-8k",
                "assistant.project.llm.front-selection.allowed-roles=superadmin,admin",
                "assistant.project.llm.models.qwen-plus.provider=qwen",
                "assistant.project.llm.models.qwen-plus.endpoint=https://example.com/qwen",
                "assistant.project.llm.models.qwen-plus.api-key=qwen-key",
                "assistant.project.llm.models.qwen-plus.model=qwen-plus",
                "assistant.project.llm.models.qwen-plus.enabled=true",
                "assistant.project.llm.models.glm-4-flash.provider=glm",
                "assistant.project.llm.models.glm-4-flash.endpoint=https://example.com/glm",
                "assistant.project.llm.models.glm-4-flash.api-key=glm-key",
                "assistant.project.llm.models.glm-4-flash.model=glm-4-flash",
                "assistant.project.llm.models.glm-4-flash.enabled=true",
                "assistant.project.llm.models.kimi-8k.provider=kimi",
                "assistant.project.llm.models.kimi-8k.endpoint=https://example.com/kimi",
                "assistant.project.llm.models.kimi-8k.api-key=kimi-key",
                "assistant.project.llm.models.kimi-8k.model=moonshot-v1-8k",
                "assistant.project.llm.models.kimi-8k.enabled=true",
                "assistant.project.llm.models.deepseek-chat.enabled=false"
        }
)
@AutoConfigureMockMvc
class ProjectAssistantControllerIntegrationTest {

    private static final String DEVICE = "PC";
    private static final AtomicLong TEST_USER_ID_SEQUENCE = new AtomicLong(50000L);

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @EnableConfigurationProperties(ProjectAssistantProperties.class)
    @org.mybatis.spring.annotation.MapperScan(basePackageClasses = {
            AiConversationMapper.class,
            AiMessageMapper.class
    })
    @Import({
            ProjectAssistantController.class,
            ProjectAssistantService.class,
            AiConversationService.class,
            LlmRoutingService.class,
            SaTokenConfig.class,
            StpInterfaceImpl.class,
            GlobalExceptionHandler.class
    })
    static class TestApp {

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
            return interceptor;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AiConversationService aiConversationService;

    @MockBean
    private ProjectKnowledgeBaseService knowledgeBaseService;

    @MockBean
    private ProjectKnowledgeRetrievalService retrievalService;

    @MockBean
    private LlmProviderRegistry providerRegistry;

    @MockBean
    private LlmGovernanceService llmGovernanceService;

    @MockBean
    private LlmAuditService llmAuditService;

    @MockBean
    private SysErrorLogMapper sysErrorLogMapper;

    @BeforeEach
    void setUpDatabase() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS ai_message");
        jdbcTemplate.execute("DROP TABLE IF EXISTS ai_conversation");
        jdbcTemplate.execute("""
                CREATE TABLE ai_conversation (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    title VARCHAR(100),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE ai_message (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    conversation_id BIGINT NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    content VARCHAR(4000) NOT NULL,
                    sources_json CLOB,
                    hit_type VARCHAR(30),
                    provider_code VARCHAR(32),
                    model_code VARCHAR(64),
                    fallback_used BOOLEAN,
                    created_at TIMESTAMP
                )
                """);

        when(knowledgeBaseService.getAllChunks()).thenReturn(List.of());
    }

    @Test
    void models_shouldReturnConfiguredModelsForAdmin() throws Exception {
        LoginSession loginSession = loginWithRole("admin", "warehouse");

        mockMvc.perform(get("/assistant/project/models")
                        .header("Authorization", "Bearer " + loginSession.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].modelCode").value("qwen-plus"))
                .andExpect(jsonPath("$.data[1].modelCode").value("glm-4-flash"))
            .andExpect(jsonPath("$.data[2].modelCode").value("kimi-8k"));
    }

    @Test
    void query_shouldFallbackToGlmAndPersistHistoryMetadata() throws Exception {
        LoginSession loginSession = loginWithRole("admin", "warehouse");
        AiConversation conversation = aiConversationService.createConversation(loginSession.userId(), "回退验证");
        String question = "库存预警图表没有覆盖到的业务怎么办？";
        LlmChatClient chatClient = mock(LlmChatClient.class);

        when(retrievalService.retrieve(question, "admin", "warehouse")).thenReturn(List.of());
        when(retrievalService.isBelowThreshold(List.of())).thenReturn(true);
        when(retrievalService.isLikelyProjectQuestion(question)).thenReturn(true);
        when(llmGovernanceService.checkUserRateLimit(anyLong())).thenReturn(LlmGovernanceService.RateLimitDecision.allowed());
        when(llmGovernanceService.tryConsumeModelBudget("qwen-plus")).thenReturn(true);
        when(llmGovernanceService.tryConsumeModelBudget("glm-4-flash")).thenReturn(true);
        when(providerRegistry.getClient(LlmProviderType.QWEN)).thenReturn(chatClient);
        when(providerRegistry.getClient(LlmProviderType.GLM)).thenReturn(chatClient);
        when(chatClient.chat(any(LlmChatRequest.class))).thenAnswer(invocation -> {
            LlmChatRequest request = invocation.getArgument(0, LlmChatRequest.class);
            if ("qwen".equals(request.getProviderCode())) {
                throw new RuntimeException("qwen unavailable");
            }
            LlmChatResponse response = new LlmChatResponse();
            response.setContent("已自动切换到 GLM 完成回答");
            response.setProviderCode(request.getProviderCode());
            response.setModelCode(request.getModelCode());
            return response;
        });

        mockMvc.perform(post("/assistant/project/query")
                        .header("Authorization", "Bearer " + loginSession.token())
                        .contentType("application/json")
                        .content("""
                                {
                                  "question": "库存预警图表没有覆盖到的业务怎么办？",
                                  "mode": "hybrid",
                                  "modelCode": "qwen-plus",
                                  "conversationId": %d
                                }
                                """.formatted(conversation.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.hitType").value("kb-miss-model"))
                .andExpect(jsonPath("$.data.providerCode").value("glm"))
                .andExpect(jsonPath("$.data.modelCode").value("glm-4-flash"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(true));

        List<AiMessage> messages = aiConversationService.getConversationMessages(conversation.getId(), loginSession.userId());
        org.junit.jupiter.api.Assertions.assertEquals(2, messages.size());
        org.junit.jupiter.api.Assertions.assertEquals("user", messages.get(0).getRole());
        org.junit.jupiter.api.Assertions.assertEquals("assistant", messages.get(1).getRole());
        org.junit.jupiter.api.Assertions.assertEquals("kb-miss-model", messages.get(1).getHitType());
        org.junit.jupiter.api.Assertions.assertEquals("glm", messages.get(1).getProviderCode());
        org.junit.jupiter.api.Assertions.assertEquals("glm-4-flash", messages.get(1).getModelCode());
        org.junit.jupiter.api.Assertions.assertTrue(Boolean.TRUE.equals(messages.get(1).getFallbackUsed()));
    }

    @Test
    void query_shouldReturnBudgetBlockedAndPersistAssistantMessage() throws Exception {
        LoginSession loginSession = loginWithRole("admin", "warehouse");
        AiConversation conversation = aiConversationService.createConversation(loginSession.userId(), "预算验证");
        String question = "帮我补充一个知识库没有覆盖的问题";

        when(retrievalService.retrieve(question, "admin", "warehouse")).thenReturn(List.of());
        when(retrievalService.isBelowThreshold(List.of())).thenReturn(true);
        when(retrievalService.isLikelyProjectQuestion(question)).thenReturn(true);
        when(llmGovernanceService.checkUserRateLimit(anyLong())).thenReturn(LlmGovernanceService.RateLimitDecision.allowed());
        when(llmGovernanceService.tryConsumeModelBudget("qwen-plus")).thenReturn(false);
        when(llmGovernanceService.tryConsumeModelBudget("glm-4-flash")).thenReturn(false);
        when(llmGovernanceService.tryConsumeModelBudget("kimi-8k")).thenReturn(false);

        mockMvc.perform(post("/assistant/project/query")
                        .header("Authorization", "Bearer " + loginSession.token())
                        .contentType("application/json")
                        .content("""
                                {
                                  "question": "帮我补充一个知识库没有覆盖的问题",
                                  "mode": "hybrid",
                                  "modelCode": "qwen-plus",
                                  "conversationId": %d
                                }
                                """.formatted(conversation.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.hitType").value("budget-blocked"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("千问 Plus 今日预算已达上限")));

        verifyNoInteractions(providerRegistry);

        List<AiMessage> messages = aiConversationService.getConversationMessages(conversation.getId(), loginSession.userId());
        org.junit.jupiter.api.Assertions.assertEquals(2, messages.size());
        org.junit.jupiter.api.Assertions.assertEquals("budget-blocked", messages.get(1).getHitType());
        org.junit.jupiter.api.Assertions.assertNull(messages.get(1).getProviderCode());
        org.junit.jupiter.api.Assertions.assertNull(messages.get(1).getModelCode());
        org.junit.jupiter.api.Assertions.assertNull(messages.get(1).getFallbackUsed());
    }

    private LoginSession loginWithRole(String role, String deptCode) {
        long userId = TEST_USER_ID_SEQUENCE.incrementAndGet();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        try {
            StpUtil.login(userId, DEVICE);
            String token = StpUtil.getTokenValueByLoginId(userId, DEVICE);
            StpUtil.getSessionByLoginId(userId).set("role", role);
            StpUtil.getSessionByLoginId(userId).set("deptCode", deptCode);
            return new LoginSession(userId, token);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private record LoginSession(long userId, String token) {
    }
}