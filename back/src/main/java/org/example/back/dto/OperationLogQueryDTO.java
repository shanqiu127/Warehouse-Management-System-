package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogQueryDTO extends PageQuery {

    private String username;

    private String module;

    private String action;

    private String targetType;

    private LocalDate startDate;

    private LocalDate endDate;
}
