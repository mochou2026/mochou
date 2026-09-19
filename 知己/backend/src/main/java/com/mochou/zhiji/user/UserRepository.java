package com.mochou.zhiji.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * user 表的读写入口。
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public void insert(String username, String encodedPassword) {
        jdbcTemplate.update(
                "INSERT INTO `user` (username, password) VALUES (?, ?)", username, encodedPassword);
    }

    public Optional<UserRecord> findByUsername(String username) {
        List<UserRecord> rows = jdbcTemplate.query(
                "SELECT id, username, password FROM `user` WHERE username = ?",
                (rs, rowNum) -> new UserRecord(rs.getLong("id"), rs.getString("username"), rs.getString("password")),
                username);
        return rows.stream().findFirst();
    }

    public void updatePassword(long id, String encodedPassword) {
        jdbcTemplate.update(
                "UPDATE `user` SET password = ?, update_time = NOW() WHERE id = ?", encodedPassword, id);
    }
}
