package com.tripsync.dao;

import com.tripsync.model.User;
import com.tripsync.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findByEmail(String email) throws SQLException {
        return JdbcUtil.queryOne(
                "SELECT * FROM users WHERE email = ?",
                this::mapUser,
                email
        );
    }

    public User findById(long id) throws SQLException {
        return JdbcUtil.queryOne(
                "SELECT * FROM users WHERE id = ?",
                this::mapUser,
                id
        );
    }

    public long create(User user) throws SQLException {
        return JdbcUtil.insert(
                "INSERT INTO users (name, email, password_hash, phone) VALUES (?, ?, ?, ?)",
                user.getName(), user.getEmail(), user.getPasswordHash(), user.getPhone()
        );
    }

    public void setResetToken(long userId, String token, LocalDateTime expires) throws SQLException {
        JdbcUtil.update(
                "UPDATE users SET reset_token = ?, reset_token_expires = ? WHERE id = ?",
                token, Timestamp.valueOf(expires), userId
        );
    }

    public User findByResetToken(String token) throws SQLException {
        return JdbcUtil.queryOne(
                "SELECT * FROM users WHERE reset_token = ? AND reset_token_expires > NOW()",
                this::mapUser,
                token
        );
    }

    public void updatePassword(long userId, String hash) throws SQLException {
        JdbcUtil.update(
                "UPDATE users SET password_hash = ?, reset_token = NULL, reset_token_expires = NULL WHERE id = ?",
                hash, userId
        );
    }

    public List<User> searchByNameOrEmail(String query) throws SQLException {
        String like = "%" + query + "%";
        List<User> users = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM users WHERE name LIKE ? OR email LIKE ? LIMIT 20")) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }
        return users;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPhone(rs.getString("phone"));
        u.setAvatarUrl(rs.getString("avatar_url"));
        u.setEmailVerified(rs.getBoolean("email_verified"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            u.setCreatedAt(created.toLocalDateTime());
        }
        return u;
    }
}
