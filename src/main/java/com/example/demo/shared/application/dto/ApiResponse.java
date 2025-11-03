package com.example.demo.shared.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 通用的 API 回應包裝器
 *
 * @param success   是否成功
 * @param message   回應訊息
 * @param data      回應資料
 * @param errorCode 錯誤碼
 * @param <T>       資料的泛型類型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String errorCode
) {
    /**
     * 建立一個成功的API回應，包含資料。
     *
     * @param data 回應的資料
     * @param <T>  資料的泛型類型
     * @return 包含資料的成功 API 回應
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data, null);
    }

    /**
     * 建立一個成功的API回應，包含自訂訊息和資料。
     *
     * @param message 自訂的成功訊息
     * @param data    回應的資料
     * @param <T>     資料的泛型類型
     * @return 包含訊息和資料的成功 API 回應
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    /**
     * 建立一個成功的API回應，僅包含訊息。
     *
     * @param message 成功訊息
     * @param <T>     資料的泛型類型
     * @return 僅包含訊息的成功 API 回應
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * 建立一個錯誤的API回應，包含錯誤訊息。
     *
     * @param message 錯誤訊息
     * @param <T>     資料的泛型類型
     * @return 包含錯誤訊息的 API 回應
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }

    /**
     * 建立一個錯誤的API回應，包含錯誤訊息和錯誤碼。
     *
     * @param message   錯誤訊息
     * @param errorCode 錯誤碼
     * @param <T>       資料的泛型類型
     * @return 包含錯誤訊息和錯誤碼的 API 回應
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }
}
