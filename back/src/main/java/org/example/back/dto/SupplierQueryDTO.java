package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierQueryDTO extends PageQuery {

    private String supplierName;

    private String contact;

    private Integer status;
}