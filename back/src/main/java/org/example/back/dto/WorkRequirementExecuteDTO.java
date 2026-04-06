package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class WorkRequirementExecuteDTO {

    private Long assignId;

    @NotBlank(message = "执行结果不能为空")
    private String executeResult;

    /** 已保留的历史附件 ID（可选） */
    private List<Long> existingAttachmentIds;

    /** 当前会话中新上传附件的临时令牌列表（可选） */
    private List<String> newAttachmentTokens;
}
