package com.calendarbox.backend.global.dto;

public record ApiResponse<T>(
        int code,
        String status,
        String message,
        T data
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, "OK", message, data);
    }
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, "CREATED", message, data);
    }
    public static ApiResponse<Void> noContent(String message) {
        return new ApiResponse<>(204, "NO_CONTENT", message, null);
    }
}
