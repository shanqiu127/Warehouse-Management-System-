package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesReturnVO {

    private Long id;

    private String returnNo;

    private String salesNo;

    private Long goodsId;

    private String goodsName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private BigDecimal refundAmount;

    private LocalDateTime operationTime;

    private LocalDateTime returnDate;

    private String operatorName;

    private String operator;

    private String remark;

    private String reason;

    private Integer bizStatus;

    private Long sourceId;

    private LocalDateTime voidTime;

    private String voidReason;

    private LocalDateTime createTime;
}
