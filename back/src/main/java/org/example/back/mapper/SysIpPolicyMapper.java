package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.back.entity.SysIpPolicy;

import java.util.List;

public interface SysIpPolicyMapper extends BaseMapper<SysIpPolicy> {

    SysIpPolicy selectDetailById(Long id);

    List<SysIpPolicy> selectEnabledPolicies();
}
