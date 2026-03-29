package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.dto.LoginLogQueryDTO;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.OperationLogQueryDTO;
import org.example.back.entity.SysLoginLog;
import org.example.back.entity.SysOperationLog;
import org.example.back.mapper.SysLoginLogMapper;
import org.example.back.mapper.SysOperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuditService {

    @Autowired
    private SysLoginLogMapper sysLoginLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthzService authzService;

    public PageResult<SysLoginLog> loginLogPage(LoginLogQueryDTO queryDTO) {
        requireSuperAdmin();
        LocalDateTime startTime = queryDTO.getStartDate() == null ? null : queryDTO.getStartDate().atStartOfDay();
        LocalDateTime endTime = queryDTO.getEndDate() == null ? null : queryDTO.getEndDate().plusDays(1).atStartOfDay();

        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getUsername()), SysLoginLog::getUsername, queryDTO.getUsername())
                .like(StringUtils.hasText(queryDTO.getIp()), SysLoginLog::getIp, queryDTO.getIp())
                .eq(queryDTO.getSuccessFlag() != null, SysLoginLog::getSuccessFlag, queryDTO.getSuccessFlag())
                .ge(startTime != null, SysLoginLog::getLoginTime, startTime)
                .lt(endTime != null, SysLoginLog::getLoginTime, endTime)
                .orderByDesc(SysLoginLog::getLoginTime)
                .orderByDesc(SysLoginLog::getId);

        Page<SysLoginLog> page = sysLoginLogMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public SysLoginLog getLoginLogById(Long id) {
        requireSuperAdmin();
        SysLoginLog log = sysLoginLogMapper.selectById(id);
        if (log == null) {
            throw BusinessException.notFound("登录日志不存在");
        }
        return log;
    }

    public PageResult<SysOperationLog> operationLogPage(OperationLogQueryDTO queryDTO) {
        requireSuperAdmin();
        LocalDateTime startTime = queryDTO.getStartDate() == null ? null : queryDTO.getStartDate().atStartOfDay();
        LocalDateTime endTime = queryDTO.getEndDate() == null ? null : queryDTO.getEndDate().plusDays(1).atStartOfDay();

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getUsername()), SysOperationLog::getUsername, queryDTO.getUsername())
                .like(StringUtils.hasText(queryDTO.getModule()), SysOperationLog::getModule, queryDTO.getModule())
                .like(StringUtils.hasText(queryDTO.getAction()), SysOperationLog::getAction, queryDTO.getAction())
                .like(StringUtils.hasText(queryDTO.getTargetType()), SysOperationLog::getTargetType, queryDTO.getTargetType())
                .ge(startTime != null, SysOperationLog::getCreateTime, startTime)
                .lt(endTime != null, SysOperationLog::getCreateTime, endTime)
                .orderByDesc(SysOperationLog::getCreateTime)
                .orderByDesc(SysOperationLog::getId);

        Page<SysOperationLog> page = sysOperationLogMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public SysOperationLog getOperationLogById(Long id) {
        requireSuperAdmin();
        SysOperationLog log = sysOperationLogMapper.selectById(id);
        if (log == null) {
            throw BusinessException.notFound("操作日志不存在");
        }
        return log;
    }

    private void requireSuperAdmin() {
        authzService.requireSuperAdmin("仅超级管理员可访问审计日志模块");
    }
}
