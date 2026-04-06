package org.example.back.controller;

import org.example.back.common.exception.GlobalExceptionHandler;
import org.example.back.config.SaTokenConfig;
import org.example.back.service.WorkRequirementAttachmentStorageService;
import org.example.back.service.WorkRequirementService;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = WorkRequirementAttachmentAuthTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
class WorkRequirementAttachmentAuthTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @Import({
            FileUploadController.class,
            SaTokenConfig.class,
            GlobalExceptionHandler.class
    })
    static class TestApp {
    }

    @MockBean
    private WorkRequirementService workRequirementService;

    @MockBean
    private WorkRequirementAttachmentStorageService attachmentStorageService;

    @MockBean
    private org.example.back.mapper.SysErrorLogMapper sysErrorLogMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @Test
    void downloadAttachment_shouldRejectAnonymousAccess() throws Exception {
        mockMvc.perform(get("/upload/work-requirement/attachments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(workRequirementService, attachmentStorageService);
    }
}