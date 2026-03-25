package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_ip_policy")
public class SysIpPolicy {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String policyName;

    private String ipCidr;

    private Integer allowFlag;

    private Integer status;

    private Integer priority;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
