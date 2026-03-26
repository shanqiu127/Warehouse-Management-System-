package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("base_goods")
public class BaseGoods {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String goodsCode;

    private String goodsName;

    private String category;

    private String brand;

    private Long supplierId;

    private BigDecimal purchasePrice;

    private BigDecimal salePrice;

    private Integer stock;

    private Integer warningStock;

    private String unit;

    private Integer status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}