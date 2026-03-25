package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.back.entity.SysLoginLog;

public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    SysLoginLog selectDetailById(Long id);
}
