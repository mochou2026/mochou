package com.mochou.zhiji.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 反馈 / 投诉提交内容。
 */
public record FeedbackRequest(
        @NotBlank(message = "请填写反馈内容")
        @Size(max = 1000, message = "反馈内容不能超过 1000 字")
        String content,

        /** 提交人用户名，未登录可为空 */
        String username) {
}
