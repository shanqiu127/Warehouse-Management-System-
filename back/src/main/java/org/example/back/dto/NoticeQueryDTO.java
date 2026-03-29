package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeQueryDTO extends PageQuery {

    private String title;

    private String targetRole;

    private Long targetDeptId;

    private Integer status;
}