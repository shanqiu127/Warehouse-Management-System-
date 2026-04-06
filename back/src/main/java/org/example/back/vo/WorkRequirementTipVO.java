package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkRequirementTipVO {

    private Long assignId;

    private String content;

    private Integer status;

    private String statusLabel;

    private Integer overdueFlag;

    private Boolean overdueCurrent;

    private Boolean lateSubmission;

    private String overdueLabel;

    private LocalDateTime endTime;
}
