package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkRequirementDetailVO {

    private Long id;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String targetScope;

    private String creatorName;

    private LocalDateTime createTime;

    private List<AssignItemVO> assigns;

    @Data
    public static class AssignItemVO {
        private Long assignId;
        private Long employeeUserId;
        private String employeeName;
        private Integer status;
        private String statusLabel;
        private Integer overdueFlag;
        private LocalDateTime overdueAt;
        private Integer submittedOnTime;
        private LocalDateTime completedAt;
        private Boolean overdueCurrent;
        private Boolean lateSubmission;
        private String overdueLabel;
        private String executeResult;
        private Integer rejectCount;
        private LocalDateTime acceptedAt;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private String reviewerName;
        private List<AttachmentVO> attachments;
    }

    @Data
    public static class AttachmentVO {
        private Long id;
        private String fileName;
        private String filePath;
        private Long fileSize;
    }
}
