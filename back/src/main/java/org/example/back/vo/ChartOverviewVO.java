package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChartOverviewVO {

    private BigDecimal salesAmount;

    private BigDecimal returnAmount;

    private BigDecimal netSalesAmount;

    private Long salesQuantity;

    private Long returnQuantity;
}
