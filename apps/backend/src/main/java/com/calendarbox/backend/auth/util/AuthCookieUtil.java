package com.calendarbox.backend.auth.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

public class AuthCookieUtil {
    private static final String ACCESS_COOKIE = "access_token";
    private static final String REFRESH_COOKIE = "refresh_token";

    public static boolean isLocal(HttpServletRequest req) {
        String host = req.getServerName();
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    public static ResponseCookie buildAccessCookie(String token, boolean local) {
        return ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(!local)
                .sameSite(local ? "Lax" : "None")
                .path("/")
                .maxAge(30 * 60) // 30분
                .build();
    }

    public static ResponseCookie buildRefreshCookie(String token, boolean local) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(!local)
                .sameSite(local ? "Lax" : "None")
                .path("/api/auth")
                .maxAge(14L * 24 * 60 * 60) // 14일
                .build();
    }

    public static ResponseCookie deleteCookie(String name, boolean local, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(!local)
                .sameSite(local ? "Lax" : "None")
                .path(path)
                .maxAge(0)
                .build();
    }
}
