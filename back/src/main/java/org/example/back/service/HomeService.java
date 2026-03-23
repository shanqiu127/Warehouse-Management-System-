package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.dto.LoginResponse;
import org.example.back.entity.SysErrorLog;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysErrorLogMapper;
import org.example.back.mapper.SysUserMapper;
import org.example.back.vo.ErrorLogBriefVO;
import org.example.back.vo.HomeSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HomeService {

    @Autowired
    private AuthService authService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysErrorLogMapper sysErrorLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public HomeSummaryVO summary() {
        LoginResponse.UserInfoVO userInfo = authService.getUserInfo();
        SysUser user = sysUserMapper.selectById(userInfo.getId());
        // 构建首页统计信息
        HomeSummaryVO vo = new HomeSummaryVO();
        vo.setUserId(userInfo.getId());
        vo.setUsername(userInfo.getUsername());
        vo.setRealName(userInfo.getRealName());
        vo.setRole(userInfo.getRole());
        vo.setCurrentLoginTime(user == null ? null : user.getCurrentLoginTime());
        vo.setLastLoginTime(user == null ? null : user.getLastLoginTime());
        vo.setServerTime(LocalDateTime.now());
        // 超级管理员可以看到数据库状态和错误日志统计
        String role = userInfo.getRole();
        if ("superadmin".equalsIgnoreCase(role)) {
            vo.setDbStatus(checkDbStatus());
        }
        if ("superadmin".equalsIgnoreCase(role)) {
            vo.setErrorCount24h(countErrorLogsLast24h());
            vo.setRecentErrorLogs(queryRecentErrorLogs(5));
        }

        return vo;
    }
    // 检查数据库连接状态
    private String checkDbStatus() {
        try {
            Integer v = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return v != null && v == 1 ? "正常" : "异常";
        } catch (Exception e) {
            return "异常";
        }
    }

    private Long countErrorLogsLast24h() {
        LocalDateTime begin = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<SysErrorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SysErrorLog::getCreateTime, begin)
                .ge(SysErrorLog::getStatusCode, 500);
        return sysErrorLogMapper.selectCount(wrapper);
    }
    // 查询最近的错误日志列表
    private List<ErrorLogBriefVO> queryRecentErrorLogs(int limit) {
        LambdaQueryWrapper<SysErrorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SysErrorLog::getStatusCode, 500)
                .orderByDesc(SysErrorLog::getCreateTime)
                .last("LIMIT " + Math.max(limit, 1));
        // 将 SysErrorLog 实体转换为 ErrorLogBriefVO 对象，并截取消息内容以适合首页展示
        return sysErrorLogMapper.selectList(wrapper).stream().map(item -> {
            ErrorLogBriefVO vo = new ErrorLogBriefVO();
            vo.setRequestUri(item.getRequestUri());
            vo.setMethod(item.getMethod());
            vo.setStatusCode(item.getStatusCode());
            vo.setErrorType(item.getErrorType());
            vo.setMessage(safeMessageForHome(item.getMessage()));
            vo.setCreateTime(item.getCreateTime());
            return vo;
        }).toList();
    }
    // 截取错误消息内容以适合首页展示，避免过长文本影响布局
    private String safeMessageForHome(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 120 ? message.substring(0, 120) + "..." : message;
    }
}
