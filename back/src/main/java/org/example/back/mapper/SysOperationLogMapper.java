package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.back.entity.SysOperationLog;

public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    SysOperationLog selectDetailById(Long id);
}
