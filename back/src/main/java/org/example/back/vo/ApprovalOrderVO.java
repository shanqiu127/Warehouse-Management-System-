package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalOrderVO {

    private Long id;

    private String approvalNo;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private String requestAction;

    private String requestReason;

    private Integer beforeBizStatus;

    private String beforeBizSnapshot;

    private Integer afterBizStatus;

    private String afterBizSnapshot;

    private Integer status;

    private Long requesterId;

    private String requesterName;

    private String requesterRole;

    private Long approverId;

    private String approverName;

    private String approveRemark;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createTime;
}
