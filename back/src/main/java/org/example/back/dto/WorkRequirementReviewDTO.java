package org.example.back.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkRequirementReviewDTO {

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
}
