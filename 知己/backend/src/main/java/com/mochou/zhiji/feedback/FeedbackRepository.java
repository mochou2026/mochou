package com.mochou.zhiji.feedback;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 反馈 / 投诉落库（mochou 库的 feedback 表）。
 */
@Repository
public class FeedbackRepository {

    private final JdbcTemplate jdbcTemplate;

    public FeedbackRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String username, String content) {
        jdbcTemplate.update(
                "INSERT INTO `feedback` (username, content) VALUES (?, ?)",
                username == null ? "" : username, content);
    }
}
