package org.example.back.llm.dto;

import lombok.Data;

@Data
public class LlmModelOption {

    private String providerCode;

    private String modelCode;

    private boolean defaultSelected;
}