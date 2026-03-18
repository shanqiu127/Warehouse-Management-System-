package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeptQueryDTO extends PageQuery {

    private String deptName;
}