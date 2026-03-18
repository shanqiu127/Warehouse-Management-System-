package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseVO {

    private Long id;

    private String purchaseNo;

    private String orderNo;

    private Long goodsId;

    private String goodsName;

    private String supplierName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal price;

    private BigDecimal totalPrice;

    private BigDecimal totalAmount;

    private LocalDateTime operationTime;

    private LocalDateTime purchaseDate;

    private String operatorName;

    private String operator;

    private String remark;

    private Integer bizStatus;

    private Long sourceId;

    private LocalDateTime voidTime;

    private String voidReason;

    private LocalDateTime createTime;
}
