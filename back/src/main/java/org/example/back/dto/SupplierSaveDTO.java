package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierSaveDTO {

    @NotBlank(message = "供应商名称不能为空")
    private String supplierName;

    private String contactPerson;

    private String contactPhone;

    private String address;

    private Integer status;

    private String description;
}