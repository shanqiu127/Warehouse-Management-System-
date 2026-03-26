package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GoodsVO {

    private Long id;

    private String goodsCode;

    private String goodsName;

    private String category;

    private String brand;

    private Long supplierId;

    private String supplierName;

    private BigDecimal purchasePrice;

    private BigDecimal salePrice;

    private BigDecimal price;

    private Integer stock;

    private Integer warningStock;

    private String unit;

    private Integer status;

    private String description;

    private LocalDateTime createTime;
}