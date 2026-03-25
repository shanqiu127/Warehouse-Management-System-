package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQueryDTO extends PageQuery {

    private String username;

    private String ip;

    private Integer successFlag;

    private LocalDate startDate;

    private LocalDate endDate;
}
