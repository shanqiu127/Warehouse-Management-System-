package org.example.back.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "assistant.project")
public class ProjectAssistantProperties {

    private boolean enabled = true;
    private List<String> docPaths = List.of("../README.md", "../AImd", "../projectmd");
    private int maxChunks = 4;
    private int maxSnippetLength = 500;
    private boolean allowRebuild = true;

    private DeepSeekConfig deepseek = new DeepSeekConfig();
    private LlmConfig llm = new LlmConfig();
    private GeneralQueryConfig general = new GeneralQueryConfig();

    @Data
    public static class DeepSeekConfig {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private double temperature = 0.3;
        private int maxTokens = 800;
    }

    @Data
    public static class LlmConfig {
        private String defaultModel = "deepseek-chat";
        private List<String> fallbackChain = List.of();
        private FrontSelectionConfig frontSelection = new FrontSelectionConfig();
        private AuditConfig audit = new AuditConfig();
        private RateLimitConfig rateLimit = new RateLimitConfig();
        private Map<String, BudgetConfig> budget = new LinkedHashMap<>();
        private Map<String, ModelConfig> models = new LinkedHashMap<>();
    }

    @Data
    public static class FrontSelectionConfig {
        private List<String> allowedRoles = List.of("superadmin", "admin");
    }

    @Data
    public static class AuditConfig {
        private boolean enabled = true;
    }

    @Data
    public static class RateLimitConfig {
        private int userPerMinute = 10;
        private int userPerHour = 100;
    }

    @Data
    public static class BudgetConfig {
        private int dailyRequests = 0;
    }

    @Data
    public static class ModelConfig {
        private String provider = "deepseek";
        private String endpoint = "";
        private String apiKey = "";
        private String model = "";
        private double temperature = 0.3;
        private int maxTokens = 800;
        private boolean enabled = true;
    }

    @Data
    public static class GeneralQueryConfig {
        private boolean enabled = true;
        private int maxQuestionLength = 240;
        private int maxEstimatedTokens = 900;
        private int maxResponseTokens = 300;
        private List<String> allowedRoles = List.of("superadmin", "admin");
    }
}
