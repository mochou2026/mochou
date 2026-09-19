package com.mochou.zhiji.common;

/**
 * 统一响应体，保持与前端约定的 {code, msg, data} 结构。
 *
 * <p>约定：HTTP 状态码始终为 200，业务结果由 body 里的 code 表达：
 * 200 成功，400 参数/业务错误，500 服务端异常。</p>
 */
public record ApiResponse<T>(int code, String msg, T data) {

    public static final int CODE_SUCCESS = 200;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_SERVER_ERROR = 500;

    /** 成功并携带数据。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(CODE_SUCCESS, "操作成功", data);
    }

    /** 成功，同时自定义提示语并携带数据。 */
    public static <T> ApiResponse<T> ok(String msg, T data) {
        return new ApiResponse<>(CODE_SUCCESS, msg, data);
    }

    /** 成功，只有提示语，没有数据。 */
    public static ApiResponse<Void> okMsg(String msg) {
        return new ApiResponse<>(CODE_SUCCESS, msg, null);
    }

    /** 业务/参数错误。 */
    public static <T> ApiResponse<T> fail(String msg) {
        return new ApiResponse<>(CODE_BAD_REQUEST, msg, null);
    }

    /** 服务端错误。 */
    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(CODE_SERVER_ERROR, msg, null);
    }
}
