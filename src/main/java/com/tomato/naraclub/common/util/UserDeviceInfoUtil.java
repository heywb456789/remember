package com.tomato.naraclub.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public class UserDeviceInfoUtil {

    // 인스턴스화 방지
    private UserDeviceInfoUtil() {}

    /**
     * 클라이언트 IP 추출 (프록시 헤더 우선)
     */
    public static String getClientIp(HttpServletRequest request) {
        for (String header : List.of(
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        )) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * User-Agent 기반 디바이스 유형 판별
     */
    public static String getDeviceType(String userAgent) {
        if (userAgent == null) {
            return "UNKNOWN";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobi")  || ua.contains("android") || ua.contains("iphone")) {
            return "MOBILE";
        }
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "TABLET";
        }
        return "DESKTOP";
    }

    /**
     * 널 세이프 문자열 반환
     */
    public static String getUserAgent(String s) {
        return (s == null ? "" : s);
    }
}
