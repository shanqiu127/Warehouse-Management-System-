package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseReturnVO {

    private Long id;

    private String returnNo;

    private String orderNo;

    private Long goodsId;

    private String goodsName;

    private String supplierName;

    private Integer quantity;

    private Integer returnQuantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private BigDecimal returnAmount;

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
