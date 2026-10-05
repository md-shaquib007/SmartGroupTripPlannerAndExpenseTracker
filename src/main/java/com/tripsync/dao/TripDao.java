package com.tripsync.dao;

import com.tripsync.model.Trip;
import com.tripsync.util.JdbcUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TripDao {

    private static final String TRIP_SELECT = """
            SELECT t.*, u.name AS leader_name,
                   (SELECT COUNT(*) FROM trip_members tm WHERE tm.trip_id = t.id) AS member_count,
                   (SELECT COALESCE(SUM(e.amount), 0) FROM expenses e WHERE e.trip_id = t.id) AS total_spent
            FROM trips t
            JOIN users u ON u.id = t.leader_id
            """;

    public Trip findById(long id) throws SQLException {
        return JdbcUtil.queryOne(TRIP_SELECT + " WHERE t.id = ?", this::mapTrip, id);
    }

    public Trip findByInviteCode(String code) throws SQLException {
        return JdbcUtil.queryOne(TRIP_SELECT + " WHERE t.invite_code = ?", this::mapTrip, code);
    }

    public List<Trip> findByUserId(long userId) throws SQLException {
        List<Trip> trips = new ArrayList<>();
        String sql = TRIP_SELECT + """
                JOIN trip_members tm ON tm.trip_id = t.id
                WHERE tm.user_id = ?
                ORDER BY t.start_date DESC
                """;
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(mapTrip(rs));
                }
            }
        }
        return trips;
    }

    public List<Trip> search(String query, String status) throws SQLException {
        List<Trip> trips = new ArrayList<>();
        StringBuilder sql = new StringBuilder(TRIP_SELECT + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (t.name LIKE ? OR t.destination LIKE ?)");
            String like = "%" + query + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND t.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY t.created_at DESC LIMIT 50");

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(mapTrip(rs));
                }
            }
        }
        return trips;
    }

    public long create(Trip trip) throws SQLException {
        return JdbcUtil.insert(
                """
                INSERT INTO trips (name, destination, start_date, end_date, budget, currency,
                                   max_members, description, invite_code, leader_id, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                trip.getName(), trip.getDestination(),
                Date.valueOf(trip.getStartDate()), Date.valueOf(trip.getEndDate()),
                trip.getBudget(), trip.getCurrency() != null ? trip.getCurrency() : "INR",
                trip.getMaxMembers(), trip.getDescription(), trip.getInviteCode(),
                trip.getLeaderId(), trip.getStatus().name()
        );
    }

    public void update(Trip trip) throws SQLException {
        JdbcUtil.update(
                """
                UPDATE trips SET name = ?, destination = ?, start_date = ?, end_date = ?,
                                 budget = ?, max_members = ?, description = ?, status = ?
                WHERE id = ?
                """,
                trip.getName(), trip.getDestination(),
                Date.valueOf(trip.getStartDate()), Date.valueOf(trip.getEndDate()),
                trip.getBudget(), trip.getMaxMembers(), trip.getDescription(),
                trip.getStatus().name(), trip.getId()
        );
    }

    public void delete(long id) throws SQLException {
        JdbcUtil.delete("DELETE FROM trips WHERE id = ?", id);
    }

    public void updateLeader(long tripId, long newLeaderId) throws SQLException {
        JdbcUtil.update("UPDATE trips SET leader_id = ? WHERE id = ?", newLeaderId, tripId);
    }

    private Trip mapTrip(ResultSet rs) throws SQLException {
        Trip t = new Trip();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setDestination(rs.getString("destination"));
        Date start = rs.getDate("start_date");
        if (start != null) t.setStartDate(start.toLocalDate());
        Date end = rs.getDate("end_date");
        if (end != null) t.setEndDate(end.toLocalDate());
        t.setBudget(rs.getBigDecimal("budget"));
        t.setCurrency(rs.getString("currency"));
        t.setMaxMembers(rs.getInt("max_members"));
        t.setDescription(rs.getString("description"));
        t.setInviteCode(rs.getString("invite_code"));
        t.setLeaderId(rs.getLong("leader_id"));
        t.setLeaderName(rs.getString("leader_name"));
        t.setStatus(Trip.TripStatus.valueOf(rs.getString("status")));
        t.setMemberCount(rs.getInt("member_count"));
        t.setTotalSpent(rs.getBigDecimal("total_spent"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toLocalDateTime());
        return t;
    }
}
