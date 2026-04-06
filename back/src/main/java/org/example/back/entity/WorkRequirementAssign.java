package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_requirement_assign")
public class WorkRequirementAssign {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long requirementId;

    private Long employeeUserId;

    private String employeeName;

    /** 0-待接受, 1-执行中, 2-待审核, 3-已完成, 4-拒收, 5-已驳回 */
    private Integer status;

    private Integer overdueFlag;

    private LocalDateTime overdueAt;

    private Integer submittedOnTime;

    private Integer overdueRemindCount;

    private LocalDateTime lastRemindTime;

    private LocalDateTime completedAt;

    private String executeResult;

    private Integer rejectCount;

    private LocalDateTime acceptedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private Long reviewerId;

    private String reviewerName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
