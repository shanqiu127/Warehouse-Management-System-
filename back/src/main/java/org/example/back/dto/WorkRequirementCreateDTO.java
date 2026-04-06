package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkRequirementCreateDTO {

    @NotBlank(message = "要求内容不能为空")
    private String content;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime endTime;

    @NotBlank(message = "对象范围不能为空")
    private String targetScope;

    /** targetScope='selected'时必填，指定员工用户ID列表 */
    private List<Long> employeeUserIds;
}
