package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class WorkRequirementExecuteDTO {

    private Long assignId;

    @NotBlank(message = "执行结果不能为空")
    private String executeResult;

    /** 附件路径列表（可选） */
    private List<String> attachmentPaths;
}
