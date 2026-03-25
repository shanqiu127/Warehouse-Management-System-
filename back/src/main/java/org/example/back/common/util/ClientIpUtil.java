package org.example.back.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtil {

    private static final String UNKNOWN = "unknown";

    private ClientIpUtil() {
    }
    // 获取客户端IP地址，考虑了多层代理的情况
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = firstValidHeaderIp(request, "X-Forwarded-For");
        if (isEmpty(ip)) {
            ip = firstValidHeaderIp(request, "Proxy-Client-IP");
        }
        if (isEmpty(ip)) {
            ip = firstValidHeaderIp(request, "WL-Proxy-Client-IP");
        }
        if (isEmpty(ip)) {
            ip = firstValidHeaderIp(request, "HTTP_X_FORWARDED_FOR");
        }
        if (isEmpty(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip == null ? "" : ip.trim();
    }
    // 从指定的请求头中提取第一个有效的IP地址
    private static String firstValidHeaderIp(HttpServletRequest request, String header) {
        String ip = request.getHeader(header);
        if (isEmpty(ip)) {
            return "";
        }
        int idx = ip.indexOf(',');
        if (idx > -1) {
            ip = ip.substring(0, idx);
        }
        return ip.trim();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isBlank() || UNKNOWN.equalsIgnoreCase(value);
    }
}
