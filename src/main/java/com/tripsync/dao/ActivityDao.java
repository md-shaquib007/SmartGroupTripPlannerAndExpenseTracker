package com.tripsync.dao;

import com.tripsync.model.ActivityLog;
import com.tripsync.model.Notification;
import com.tripsync.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ActivityDao {

    public void log(long tripId, Long userId, String actionType, String description) throws SQLException {
        JdbcUtil.insert(
                "INSERT INTO activity_logs (trip_id, user_id, action_type, description) VALUES (?, ?, ?, ?)",
                tripId, userId, actionType, description
        );
    }

    public List<ActivityLog> findByTripId(long tripId) throws SQLException {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = """
                SELECT al.*, u.name AS user_name
                FROM activity_logs al
                LEFT JOIN users u ON u.id = al.user_id
                WHERE al.trip_id = ?
                ORDER BY al.created_at DESC
                LIMIT 100
                """;
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActivityLog log = new ActivityLog();
                    log.setId(rs.getLong("id"));
                    log.setTripId(rs.getLong("trip_id"));
                    long uid = rs.getLong("user_id");
                    if (!rs.wasNull()) log.setUserId(uid);
                    log.setUserName(rs.getString("user_name"));
                    log.setActionType(rs.getString("action_type"));
                    log.setDescription(rs.getString("description"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) log.setCreatedAt(created.toLocalDateTime());
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    public void createNotification(long userId, Long tripId, String type, String message) throws SQLException {
        JdbcUtil.insert(
                "INSERT INTO notifications (user_id, trip_id, type, message) VALUES (?, ?, ?, ?)",
                userId, tripId, type, message
        );
    }

    public List<Notification> findUnreadByUser(long userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapNotification(rs));
                }
            }
        }
        return list;
    }

    public void markAllRead(long userId) throws SQLException {
        JdbcUtil.update("UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE", userId);
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        long tripId = rs.getLong("trip_id");
        if (!rs.wasNull()) n.setTripId(tripId);
        n.setType(rs.getString("type"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) n.setCreatedAt(created.toLocalDateTime());
        return n;
    }
}
