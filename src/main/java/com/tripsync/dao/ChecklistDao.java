package com.tripsync.dao;

import com.tripsync.model.ChecklistItem;
import com.tripsync.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChecklistDao {

    public List<ChecklistItem> findByTripId(long tripId) throws SQLException {
        List<ChecklistItem> items = new ArrayList<>();
        String sql = "SELECT * FROM checklist_items WHERE trip_id = ? ORDER BY item_name";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChecklistItem item = mapItem(rs);
                    item.setAssignments(findAssignments(item.getId()));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public long create(ChecklistItem item) throws SQLException {
        return JdbcUtil.insert(
                "INSERT INTO checklist_items (trip_id, item_name, category, created_by) VALUES (?, ?, ?, ?)",
                item.getTripId(), item.getItemName(), item.getCategory(), item.getCreatedBy()
        );
    }

    public void setAssignment(long itemId, long userId, ChecklistItem.AssignmentStatus status) throws SQLException {
        JdbcUtil.update(
                """
                INSERT INTO checklist_assignments (checklist_item_id, user_id, status)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status)
                """,
                itemId, userId, status.name()
        );
    }

    private Map<Long, ChecklistItem.AssignmentStatus> findAssignments(long itemId) throws SQLException {
        Map<Long, ChecklistItem.AssignmentStatus> map = new HashMap<>();
        String sql = "SELECT user_id, status FROM checklist_assignments WHERE checklist_item_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getLong("user_id"),
                            ChecklistItem.AssignmentStatus.valueOf(rs.getString("status")));
                }
            }
        }
        return map;
    }

    private ChecklistItem mapItem(ResultSet rs) throws SQLException {
        ChecklistItem item = new ChecklistItem();
        item.setId(rs.getLong("id"));
        item.setTripId(rs.getLong("trip_id"));
        item.setItemName(rs.getString("item_name"));
        item.setCategory(rs.getString("category"));
        item.setCreatedBy(rs.getLong("created_by"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) item.setCreatedAt(created.toLocalDateTime());
        return item;
    }
}
