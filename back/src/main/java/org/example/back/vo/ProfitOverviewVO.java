package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitOverviewVO {

    private BigDecimal salesAmount;

    private BigDecimal returnAmount;

    private BigDecimal netSalesAmount;

    private Long salesQuantity;

    private Long returnQuantity;

    private Long netQuantity;

    private BigDecimal estimatedCost;

    private BigDecimal grossProfitAmount;

    private BigDecimal grossProfitRate;
}
