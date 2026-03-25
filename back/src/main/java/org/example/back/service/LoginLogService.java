package org.example.back.service;

import org.example.back.entity.SysLoginLog;
import org.example.back.mapper.SysLoginLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogService {

    @Autowired
    private SysLoginLogMapper sysLoginLogMapper;

    public void record(Long userId,
                       String username,
                       String ip,
                       String userAgent,
                       boolean success,
                       String failReason) {
        SysLoginLog log = new SysLoginLog();
        log.setUserId(userId);
        log.setUsername(trim(username, 50));
        log.setIp(trim(ip, 64));
        log.setUserAgent(trim(userAgent, 300));
        log.setSuccessFlag(success ? 1 : 0);
        log.setFailReason(success ? null : trim(defaultReason(failReason), 200));
        log.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(log);
    }

    private String defaultReason(String failReason) {
        if (failReason == null || failReason.isBlank()) {
            return "登录失败";
        }
        return failReason;
    }

    private String trim(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
