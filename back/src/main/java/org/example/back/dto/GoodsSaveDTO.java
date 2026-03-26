package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsSaveDTO {

    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    private String category;

    private String brand;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "进价不能为空")
    @DecimalMin(value = "0.01", message = "进价必须大于0")
    private BigDecimal purchasePrice;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于0")
    private BigDecimal salePrice;

    private Integer stock;

    private Integer warningStock;

    private String unit;

    private Integer status;

    private String description;
}