package org.example.back.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class IpPolicyQueryDTO extends PageQuery {

    private String policyName;

    private Integer status;

    private Integer allowFlag;
}
