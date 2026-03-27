package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalCreateDTO {

    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @NotNull(message = "业务单据ID不能为空")
    private Long bizId;

    @NotBlank(message = "申请动作不能为空")
    private String requestAction;

    private String reason;
}
