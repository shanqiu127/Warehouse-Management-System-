package org.example.back.llm.factory;

import org.example.back.llm.client.LlmChatClient;
import org.example.back.llm.enums.LlmProviderType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmProviderRegistry {

    private final List<LlmChatClient> clients;

    public LlmProviderRegistry(List<LlmChatClient> clients) {
        this.clients = clients;
    }

    public LlmChatClient getClient(LlmProviderType providerType) {
        return clients.stream()
                .filter(client -> client.supports(providerType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到可用的 LLM client: " + providerType));
    }
}