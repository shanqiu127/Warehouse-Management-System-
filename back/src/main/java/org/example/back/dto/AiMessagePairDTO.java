package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiMessagePairDTO {

    @NotBlank(message = "用户消息不能为空")
    @Size(max = 500, message = "用户消息长度不能超过500字")
    private String userContent;

    @NotBlank(message = "助手回答不能为空")
    private String assistantContent;

    private String sourcesJson;

    @Size(max = 30, message = "hitType 长度不能超过30")
    private String hitType;

    @Size(max = 32, message = "providerCode 长度不能超过32")
    private String providerCode;

    @Size(max = 64, message = "modelCode 长度不能超过64")
    private String modelCode;

    private Boolean fallbackUsed;
}
