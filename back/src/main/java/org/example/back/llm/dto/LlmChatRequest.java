package org.example.back.llm.dto;

import lombok.Data;
import org.example.back.llm.enums.LlmProviderType;

@Data
public class LlmChatRequest {

    private LlmProviderType providerType;
    private String providerCode;
    private String endpoint;
    private String apiKey;
    private String modelCode;
    private double temperature;
    private int maxTokens;
    private String systemPrompt;
    private String userPrompt;
}