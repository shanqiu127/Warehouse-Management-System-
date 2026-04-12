package org.example.back.llm.dto;

import lombok.Data;

@Data
public class LlmChatResponse {

    private String content;
    private String providerCode;
    private String modelCode;
    private boolean fallbackUsed;
    private Long latencyMs;
}