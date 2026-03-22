package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesSourceOptionVO {

    private Long id;

    private String salesNo;

    private Long goodsId;

    private String goodsName;

    private Integer quantity;

    private Integer returnedQuantity;

    private Integer returnableQuantity;

    private BigDecimal unitPrice;

    private LocalDateTime operationTime;
}
