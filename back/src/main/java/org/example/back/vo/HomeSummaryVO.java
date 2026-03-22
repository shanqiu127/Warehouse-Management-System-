package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeSummaryVO {

    private Long userId;

    private String username;

    private String realName;

    private String role;

    private LocalDateTime currentLoginTime;

    private LocalDateTime lastLoginTime;

    private LocalDateTime serverTime;

    /**
     * 数据库连接状态：正常/异常
     */
    private String dbStatus;

    /**
     * 最近24小时系统错误日志数
     */
    private Long errorCount24h;

    /**
     * 最近系统错误日志（仅首页摘要展示）
     */
    private List<ErrorLogBriefVO> recentErrorLogs;
}
