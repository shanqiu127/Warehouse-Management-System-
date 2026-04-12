package org.example.back.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProjectAssistantAnswerVO {

    private String question;
    private String answer;
    private String reasoning;
    private List<ProjectAssistantSourceVO> sources;
    private String hitType;
    private String mode;
    private String providerCode;
    private String modelCode;
    private boolean fallbackUsed;
    private Long latencyMs;
}
