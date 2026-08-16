package com.ma_fashion_vibe_be.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientInfoUtils {

    // Lấy địa chỉ IP thật của người dùng
    public static String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Nếu có nhiều IP do đi qua proxy, lấy IP đầu tiên
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }

    // Lấy tên Trình duyệt / Thiết bị (User-Agent)
    public static String getClientDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown Device";
        }

        // Cắt ngắn bớt nếu chuỗi quá dài (để vừa với độ dài cột trong DB)
        if (userAgent.length() > 250) {
            return userAgent.substring(0, 250);
        }
        return userAgent;
    }
}