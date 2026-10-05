package com.tripsync.dao;

import com.tripsync.model.TripMember;
import com.tripsync.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TripMemberDao {

    public void addMember(long tripId, long userId, TripMember.MemberRole role) throws SQLException {
        JdbcUtil.insert(
                "INSERT INTO trip_members (trip_id, user_id, role) VALUES (?, ?, ?)",
                tripId, userId, role.name()
        );
    }

    public boolean isMember(long tripId, long userId) throws SQLException {
        Integer count = JdbcUtil.queryOne(
                "SELECT COUNT(*) AS cnt FROM trip_members WHERE trip_id = ? AND user_id = ?",
                rs -> rs.getInt("cnt"),
                tripId, userId
        );
        return count != null && count > 0;
    }

    public TripMember findMember(long tripId, long userId) throws SQLException {
        return JdbcUtil.queryOne(
                """
                SELECT tm.*, u.name AS user_name, u.email AS user_email
                FROM trip_members tm
                JOIN users u ON u.id = tm.user_id
                WHERE tm.trip_id = ? AND tm.user_id = ?
                """,
                this::mapMember,
                tripId, userId
        );
    }

    public List<TripMember> findByTripId(long tripId) throws SQLException {
        List<TripMember> members = new ArrayList<>();
        String sql = """
                SELECT tm.*, u.name AS user_name, u.email AS user_email
                FROM trip_members tm
                JOIN users u ON u.id = tm.user_id
                WHERE tm.trip_id = ?
                ORDER BY FIELD(tm.role, 'LEADER', 'CO_LEADER', 'MEMBER'), tm.joined_at
                """;
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapMember(rs));
                }
            }
        }
        return members;
    }

    public int countMembers(long tripId) throws SQLException {
        Integer count = JdbcUtil.queryOne(
                "SELECT COUNT(*) AS cnt FROM trip_members WHERE trip_id = ?",
                rs -> rs.getInt("cnt"),
                tripId
        );
        return count != null ? count : 0;
    }

    public void updateRole(long tripId, long userId, TripMember.MemberRole role) throws SQLException {
        JdbcUtil.update(
                "UPDATE trip_members SET role = ? WHERE trip_id = ? AND user_id = ?",
                role.name(), tripId, userId
        );
    }

    public void removeMember(long tripId, long userId) throws SQLException {
        JdbcUtil.delete("DELETE FROM trip_members WHERE trip_id = ? AND user_id = ?", tripId, userId);
    }

    private TripMember mapMember(ResultSet rs) throws SQLException {
        TripMember m = new TripMember();
        m.setId(rs.getLong("id"));
        m.setTripId(rs.getLong("trip_id"));
        m.setUserId(rs.getLong("user_id"));
        m.setUserName(rs.getString("user_name"));
        m.setUserEmail(rs.getString("user_email"));
        m.setRole(TripMember.MemberRole.valueOf(rs.getString("role")));
        Timestamp joined = rs.getTimestamp("joined_at");
        if (joined != null) m.setJoinedAt(joined.toLocalDateTime());
        return m;
    }
}
