package org.example.back.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseSaveDTO {

    @NotNull(message = "商品不能为空")
    private Long goodsId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private BigDecimal unitPrice;

    private LocalDateTime operationTime;

    private String remark;
}
