package org.example.back.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.back.llm.dto.LlmChatRequest;
import org.example.back.llm.dto.LlmChatResponse;
import org.example.back.llm.enums.LlmProviderType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleLlmClient implements LlmChatClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(LlmProviderType providerType) {
        return providerType == LlmProviderType.DEEPSEEK
                || providerType == LlmProviderType.QWEN
                || providerType == LlmProviderType.GLM
                || providerType == LlmProviderType.KIMI;
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", request.getModelCode());
        requestBody.put("temperature", request.getTemperature());
        requestBody.put("max_tokens", request.getMaxTokens());
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", request.getSystemPrompt()),
                Map.of("role", "user", "content", request.getUserPrompt())
        ));

        String json = objectMapper.writeValueAsString(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(request.getEndpoint()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + request.getApiKey())
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM API 调用失败: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        LlmChatResponse llmResponse = new LlmChatResponse();
        llmResponse.setContent(root.path("choices").path(0).path("message").path("content").asText(""));
        llmResponse.setProviderCode(request.getProviderCode());
        llmResponse.setModelCode(request.getModelCode());
        llmResponse.setFallbackUsed(false);
        return llmResponse;
    }
}