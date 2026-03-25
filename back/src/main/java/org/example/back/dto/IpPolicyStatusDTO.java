package org.example.back.dto;

import lombok.Data;

@Data
public class IpPolicyStatusDTO {

    // 前端可直接传 enabled(true/false) 或 status(1/0)
    private Boolean enabled;

    private Integer status;
}
