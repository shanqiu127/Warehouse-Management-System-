package org.example.back.llm.enums;

public enum LlmProviderType {
    DEEPSEEK,
    QWEN,
    GLM,
    KIMI;

    public static LlmProviderType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("LLM provider code 不能为空");
        }

        return switch (code.trim().toLowerCase()) {
            case "deepseek" -> DEEPSEEK;
            case "qwen" -> QWEN;
            case "glm" -> GLM;
            case "kimi" -> KIMI;
            default -> throw new IllegalArgumentException("不支持的 LLM provider: " + code);
        };
    }
}