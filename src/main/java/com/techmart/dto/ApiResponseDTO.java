package com.techmart.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Generic API Response wrapper used by all JAX-RS endpoints.
 * Provides a consistent envelope: {success, message, data, timestamp, errorCode}
 */
public class ApiResponseDTO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private LocalDateTime timestamp;
    private int totalCount;

    // ── Static Factory Methods ────────────────────────────────────────────────

    public static <T> ApiResponseDTO<T> success(T data) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.success   = true;
        response.data      = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponseDTO<T> success(T data, String message) {
        ApiResponseDTO<T> response = success(data);
        response.message = message;
        return response;
    }

    public static <T> ApiResponseDTO<T> success(T data, String message, int totalCount) {
        ApiResponseDTO<T> response = success(data, message);
        response.totalCount = totalCount;
        return response;
    }

    public static <T> ApiResponseDTO<T> error(String message, String errorCode) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.success   = false;
        response.message   = message;
        response.errorCode = errorCode;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponseDTO<T> error(String message) {
        return error(message, "UNKNOWN_ERROR");
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public ApiResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
