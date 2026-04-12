package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectAssistantQueryDTO {

    private Long conversationId;

    @Size(max = 64, message = "modelCode 长度不能超过64")
    private String modelCode;

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过500字")
    private String question;

    @Pattern(regexp = "^(strict|hybrid)?$", message = "回答模式不合法")
    private String mode;
}
