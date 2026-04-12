package org.example.back.common;

public enum AssistantAnswerMode {

    STRICT("strict"),
    HYBRID("hybrid");

    private final String code;

    AssistantAnswerMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AssistantAnswerMode resolve(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return HYBRID;
        }

        String normalized = rawMode.trim().toLowerCase();
        for (AssistantAnswerMode value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        return HYBRID;
    }
}