package org.example.back.llm.dto;

import lombok.Data;

@Data
public class LlmInvocationContext {

    private Long userId;

    private String roleCode;

    private String deptCode;

    private Long conversationId;

    private String sceneCode;

    private String questionType;

    private String hitType;

    private String requestedModelCode;

    private String questionExcerpt;
}