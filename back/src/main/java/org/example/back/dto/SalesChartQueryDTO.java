package org.example.back.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SalesChartQueryDTO {

    private LocalDate startDate;

    private LocalDate endDate;
}
