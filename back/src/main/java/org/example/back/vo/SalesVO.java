package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesVO {

    private Long id;

    private String salesNo;

    private Long goodsId;

    private String goodsName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal salesPrice;

    private BigDecimal totalPrice;

    private BigDecimal totalAmount;

    private LocalDateTime operationTime;

    private LocalDateTime salesDate;

    private String operatorName;

    private String operator;

    private String remark;

    private Integer bizStatus;

    private Long sourceId;

    private LocalDateTime voidTime;

    private String voidReason;

    private LocalDateTime createTime;
}
