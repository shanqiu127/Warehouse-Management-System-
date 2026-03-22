package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_sales_return")
public class BizSalesReturn {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String returnNo;

    private Long sourceSalesId;

    private String sourceSalesNo;

    private Long goodsId;

    private String goodsName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operationTime;

    private String remark;

    /**
     * 1-正常, 2-已作废, 3-红冲单
     */
    private Integer bizStatus;

    /**
     * 红冲来源单ID
     */
    private Long sourceId;

    private LocalDateTime voidTime;

    private String voidReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
