package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String deptName;

    private String deptCode;

    private String leader;

    private String phone;

    private String description;

    /**
     * 1-待审批, 2-已生效, 3-已驳回
     */
    private Integer status;

    private Long requesterId;

    private String requesterName;

    private Long approverId;

    private String approverName;

    private String approvalRemark;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}