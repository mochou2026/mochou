package com.mochou.zhiji.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 修改密码请求。
 */
public record ChangePasswordRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "请输入原密码")
        String oldPassword,

        @NotBlank(message = "请输入新密码")
        String newPassword) {
}
