package org.example.back.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.example.back.common.aspect.RequireAdminAspect;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.exception.GlobalExceptionHandler;
import org.example.back.config.SaTokenConfig;
import org.example.back.config.StpInterfaceImpl;
import org.example.back.mapper.SysErrorLogMapper;
import org.example.back.service.ApprovalService;
import org.example.back.service.AuthzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ApprovalControllerAuthTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
class ApprovalControllerAuthTest {

    private static final String DEVICE = "PC";
    private static final AtomicLong TEST_USER_ID_SEQUENCE = new AtomicLong(30000L);

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @Import({
            ApprovalController.class,
            SaTokenConfig.class,
            StpInterfaceImpl.class,
            RequireAdminAspect.class,
            GlobalExceptionHandler.class
    })
    static class TestApp {
    }

    @MockBean
    private ApprovalService approvalService;

    @MockBean
    private AuthzService authzService;

    @MockBean
    private SysErrorLogMapper sysErrorLogMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpAuthz() {
        doAnswer(invocation -> {
            String message = invocation.getArgument(0, String.class);
            Object roleObj = StpUtil.getSession().get("role");
            String role = roleObj == null ? "" : String.valueOf(roleObj).trim().toLowerCase(Locale.ROOT);
            if (!"admin".equals(role) && !"superadmin".equals(role)) {
                throw BusinessException.forbidden(message);
            }
            return null;
        }).when(authzService).requireAdminOrSuperAdmin(anyString());
    }

    @Test
    void create_shouldRejectEmployeeAccess() throws Exception {
        String token = loginWithRole("employee");

        mockMvc.perform(post("/system/approval-orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"bizType":"purchase","bizId":1,"requestAction":"void","reason":"test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(approvalService);
    }

    @Test
    void create_shouldAllowAdminAccess() throws Exception {
        String token = loginWithRole("admin");

        mockMvc.perform(post("/system/approval-orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"bizType":"purchase","bizId":1,"requestAction":"void","reason":"test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(approvalService).create(any());
    }

    private String loginWithRole(String role) {
        long userId = TEST_USER_ID_SEQUENCE.incrementAndGet();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        try {
            StpUtil.login(userId, DEVICE);
            String token = StpUtil.getTokenValueByLoginId(userId, DEVICE);
            StpUtil.getSessionByLoginId(userId).set("role", role);
            return token;
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}