package org.example.back.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovalDecisionDTO {

    @Size(max = 200, message = "审批备注长度不能超过200")
    private String remark;
}
