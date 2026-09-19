package com.mochou.zhiji.photo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 照片墙图片的读写。图片存储在独立的 chou 库的 photo 表里。
 */
@Repository
public class PhotoRepository {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String SELECT_COLUMNS =
            "SELECT id, username, filename, size_bytes, create_time FROM `chou`.`photo`";

    private final JdbcTemplate jdbcTemplate;

    public PhotoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 写入图片，返回自增主键 */
    public long insert(String username, String filename, String contentType, byte[] data) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `chou`.`photo` (username, filename, content_type, size_bytes, data) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, filename);
            ps.setString(3, contentType);
            ps.setInt(4, data.length);
            ps.setBytes(5, data);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /** 列表（最新的在前）；username 为空表示全部 */
    public List<PhotoSummary> findAll(String username, int limit) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        List<Object> args = new ArrayList<>();
        if (username != null && !username.isEmpty()) {
            sql.append(" WHERE username = ?");
            args.add(username);
        }
        sql.append(" ORDER BY id DESC LIMIT ?");
        args.add(limit);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new PhotoSummary(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("filename"),
                rs.getLong("size_bytes"),
                formatTime(rs.getTimestamp("create_time")),
                "/api/photos/" + rs.getLong("id") + "/raw"
        ), args.toArray());
    }

    public Optional<PhotoSummary> findById(long id) {
        List<PhotoSummary> rows = jdbcTemplate.query(SELECT_COLUMNS + " WHERE id = ?",
                (rs, rowNum) -> new PhotoSummary(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("filename"),
                        rs.getLong("size_bytes"),
                        formatTime(rs.getTimestamp("create_time")),
                        "/api/photos/" + rs.getLong("id") + "/raw"
                ), id);
        return rows.stream().findFirst();
    }

    public Optional<PhotoData> findData(long id) {
        List<PhotoData> rows = jdbcTemplate.query(
                "SELECT content_type, data FROM `chou`.`photo` WHERE id = ?",
                (rs, rowNum) -> new PhotoData(rs.getString("content_type"), rs.getBytes("data")),
                id);
        return rows.stream().findFirst();
    }

    public int countByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `chou`.`photo` WHERE username = ?", Integer.class, username);
        return count == null ? 0 : count;
    }

    private static String formatTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(TIME_FORMAT);
    }
}
