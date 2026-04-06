package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkRequirementQueryDTO extends PageQuery {

    private Integer status;

    private String keyword;

    private String overdueType;
}
