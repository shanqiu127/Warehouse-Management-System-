package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsQueryDTO extends PageQuery {

    private String goodsName;

    private Long supplierId;

    private Integer status;
}