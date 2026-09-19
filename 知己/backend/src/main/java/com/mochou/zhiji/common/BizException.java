package com.mochou.zhiji.common;

/**
 * 可预期的业务异常，message 会直接返回给前端。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
