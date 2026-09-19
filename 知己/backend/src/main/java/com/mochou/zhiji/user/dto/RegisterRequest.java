package com.mochou.zhiji.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求。用户名/密码的“非空 + 长度”规则在 UserService 里统一校验，
 * 这里只挡住 null 与空白。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password) {
}
