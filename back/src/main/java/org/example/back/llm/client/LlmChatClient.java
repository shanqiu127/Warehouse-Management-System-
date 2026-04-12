package org.example.back.llm.client;

import org.example.back.llm.dto.LlmChatRequest;
import org.example.back.llm.dto.LlmChatResponse;
import org.example.back.llm.enums.LlmProviderType;

public interface LlmChatClient {

    boolean supports(LlmProviderType providerType);

    LlmChatResponse chat(LlmChatRequest request) throws Exception;
}