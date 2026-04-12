package org.example.back.vo;

import lombok.Data;

@Data
public class ProjectAssistantModelOptionVO {

    private String providerCode;

    private String modelCode;

    private String label;

    private boolean defaultSelected;
}