package com.mochou.zhiji.feedback;

import com.mochou.zhiji.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反馈 / 投诉作者：POST /api/feedback
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;

    public FeedbackController(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @PostMapping
    public ApiResponse<Void> submit(@Valid @RequestBody FeedbackRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        feedbackRepository.insert(username, request.content().trim());
        return ApiResponse.okMsg("感谢你的反馈");
    }
}
