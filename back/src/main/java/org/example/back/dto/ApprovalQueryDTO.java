package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalQueryDTO extends PageQuery {

    private String approvalNo;

    private String bizType;

    private String requestAction;

    private Integer status;

    private String requesterName;
}
