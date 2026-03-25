package org.example.back.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.util.ClientIpUtil;
import org.example.back.service.IpPolicyService;
import org.example.back.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginIpWhitelistInterceptor implements HandlerInterceptor {

    @Autowired
    private IpPolicyService ipPolicyService;

    @Autowired
    private LoginLogService loginLogService;
    // 这个拦截器主要用于在用户登录时检查其IP地址是否在允许访问的范围内。如果不在范围内，则记录登录日志并抛出异常。
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = ClientIpUtil.getClientIp(request);
        IpPolicyService.IpCheckResult checkResult = ipPolicyService.checkIp(clientIp);
        if (checkResult.isAllowed()) {
            return true;
        }
        // 记录登录日志
        String userAgent = request.getHeader("User-Agent");
        loginLogService.record(null, null, clientIp, userAgent, false,
                "IP不在允许访问范围内: " + checkResult.getClientIp());
        throw BusinessException.forbidden("IP 不在允许访问范围内");
    }
}
