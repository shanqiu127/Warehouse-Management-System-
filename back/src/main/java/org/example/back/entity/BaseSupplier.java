package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("base_supplier")
public class BaseSupplier {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String supplierCode;

    private String supplierName;

    private String contactPerson;

    private String contactPhone;

    private String address;

    private Integer status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}