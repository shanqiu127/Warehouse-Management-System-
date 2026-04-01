package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeptVO {

    private Long id;

    private String deptName;

    private String deptCode;

    private String leader;

    private String manager;

    private String phone;

    private String contactPhone;

    private String description;

    private Integer status;

    private String requesterName;

    private String approverName;

    private String approvalRemark;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createTime;
}