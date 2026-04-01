package org.example.back.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.example.back.common.exception.GlobalExceptionHandler;
import org.example.back.config.SaTokenConfig;
import org.example.back.config.StpInterfaceImpl;
import org.example.back.mapper.SysErrorLogMapper;
import org.example.back.service.SalesChartService;
import org.example.back.vo.ChartOverviewVO;
import org.example.back.vo.ProfitOverviewVO;
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

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SalesChartAuthTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
class SalesChartAuthTest {

    private static final String DEVICE = "PC";
    private static final AtomicLong TEST_USER_ID_SEQUENCE = new AtomicLong(20000L);

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @Import({
            SalesChartController.class,
            SaTokenConfig.class,
            StpInterfaceImpl.class,
            GlobalExceptionHandler.class
    })
    static class TestApp {
    }

    @MockBean
    private SalesChartService salesChartService;

    @MockBean
    private SysErrorLogMapper sysErrorLogMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    private String loginWithRole(String role) {
        return loginWithRole(TEST_USER_ID_SEQUENCE.incrementAndGet(), role);
    }

    private String loginWithRole(long userId, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        try {
            StpUtil.login(userId, DEVICE);
            String token = StpUtil.getTokenValueByLoginId(userId, DEVICE);
            // 直接写入账号 Session：供 StpInterfaceImpl 读取角色列表
            StpUtil.getSessionByLoginId(userId).set("role", role);
            return token;
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void SalesChartApi_shouldRejectEmployeeDirectAccess() throws Exception {
        String token = loginWithRole("employee");

        mockMvc.perform(get("/business/charts/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void SalesChartApi_shouldAllowAdmin() throws Exception {
        when(salesChartService.getOverview(any()))
                .thenReturn(buildOverview());

        String token = loginWithRole("admin");

        mockMvc.perform(get("/business/charts/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void SalesChartApi_shouldAllowSuperAdmin() throws Exception {
        when(salesChartService.getOverview(any()))
                .thenReturn(buildOverview());

        String token = loginWithRole("superadmin");

        mockMvc.perform(get("/business/charts/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void ProfitApi_shouldAllowAdminWhenAdminOnly() throws Exception {
        when(salesChartService.getProfitOverview(any()))
            .thenReturn(buildProfitOverview());

        String token = loginWithRole("admin");

        mockMvc.perform(get("/business/charts/profit-overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
        void ProfitApi_shouldRejectSuperAdminWhenAdminOnly() throws Exception {
        String token = loginWithRole("superadmin");

        mockMvc.perform(get("/business/charts/profit-overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403));
    }

    private static ChartOverviewVO buildOverview() {
        ChartOverviewVO vo = new ChartOverviewVO();
        vo.setSalesAmount(BigDecimal.ZERO);
        vo.setReturnAmount(BigDecimal.ZERO);
        vo.setNetSalesAmount(BigDecimal.ZERO);
        vo.setSalesQuantity(0L);
        vo.setReturnQuantity(0L);
        return vo;
    }

    private static ProfitOverviewVO buildProfitOverview() {
        ProfitOverviewVO vo = new ProfitOverviewVO();
        vo.setNetSalesAmount(BigDecimal.ZERO);
        vo.setEstimatedCost(BigDecimal.ZERO);
        vo.setGrossProfitAmount(BigDecimal.ZERO);
        vo.setGrossProfitRate(BigDecimal.ZERO);
        return vo;
    }
}
