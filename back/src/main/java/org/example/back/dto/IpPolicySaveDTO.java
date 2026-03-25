package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IpPolicySaveDTO {

    @NotBlank(message = "策略名称不能为空")
    private String policyName;

    @NotBlank(message = "IP/CIDR 不能为空")
    private String ipCidr;

    @NotNull(message = "放行标记不能为空")
    private Integer allowFlag;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private Integer priority;

    private String remark;
}
