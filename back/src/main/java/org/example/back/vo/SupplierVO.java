package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierVO {

    private Long id;

    private String supplierCode;

    private String supplierName;

    private String contactPerson;

    private String contact;

    private String contactPhone;

    private String phone;

    private String address;

    private Integer status;

    private String description;

    private LocalDateTime createTime;
}