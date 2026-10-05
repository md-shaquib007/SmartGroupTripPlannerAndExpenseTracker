package com.tripsync.dao;

import com.tripsync.model.Itinerary;
import com.tripsync.model.ItineraryItem;
import com.tripsync.util.JdbcUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ItineraryDao {

    public List<Itinerary> findByTripId(long tripId) throws SQLException {
        List<Itinerary> days = new ArrayList<>();
        String sql = "SELECT * FROM itineraries WHERE trip_id = ? ORDER BY day_number";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Itinerary day = mapItinerary(rs);
                    day.setItems(findItems(day.getId()));
                    days.add(day);
                }
            }
        }
        return days;
    }

    public long createDay(Itinerary itinerary) throws SQLException {
        return JdbcUtil.insert(
                "INSERT INTO itineraries (trip_id, day_number, title, itinerary_date) VALUES (?, ?, ?, ?)",
                itinerary.getTripId(), itinerary.getDayNumber(), itinerary.getTitle(),
                itinerary.getItineraryDate() != null ? Date.valueOf(itinerary.getItineraryDate()) : null
        );
    }

    public long addItem(ItineraryItem item) throws SQLException {
        return JdbcUtil.insert(
                """
                INSERT INTO itinerary_items (itinerary_id, title, description, start_time, end_time,
                                             location, sort_order, status, suggested_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                item.getItineraryId(), item.getTitle(), item.getDescription(),
                item.getStartTime() != null ? Time.valueOf(item.getStartTime()) : null,
                item.getEndTime() != null ? Time.valueOf(item.getEndTime()) : null,
                item.getLocation(), item.getSortOrder(),
                item.getStatus() != null ? item.getStatus().name() : "APPROVED",
                item.getSuggestedBy()
        );
    }

    public void updateItemStatus(long itemId, ItineraryItem.ItemStatus status) throws SQLException {
        JdbcUtil.update("UPDATE itinerary_items SET status = ? WHERE id = ?", status.name(), itemId);
    }

    private List<ItineraryItem> findItems(long itineraryId) throws SQLException {
        List<ItineraryItem> items = new ArrayList<>();
        String sql = "SELECT * FROM itinerary_items WHERE itinerary_id = ? ORDER BY sort_order, id";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, itineraryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
            }
        }
        return items;
    }

    private Itinerary mapItinerary(ResultSet rs) throws SQLException {
        Itinerary i = new Itinerary();
        i.setId(rs.getLong("id"));
        i.setTripId(rs.getLong("trip_id"));
        i.setDayNumber(rs.getInt("day_number"));
        i.setTitle(rs.getString("title"));
        Date d = rs.getDate("itinerary_date");
        if (d != null) i.setItineraryDate(d.toLocalDate());
        return i;
    }

    private ItineraryItem mapItem(ResultSet rs) throws SQLException {
        ItineraryItem item = new ItineraryItem();
        item.setId(rs.getLong("id"));
        item.setItineraryId(rs.getLong("itinerary_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        Time start = rs.getTime("start_time");
        if (start != null) item.setStartTime(start.toLocalTime());
        Time end = rs.getTime("end_time");
        if (end != null) item.setEndTime(end.toLocalTime());
        item.setLocation(rs.getString("location"));
        item.setSortOrder(rs.getInt("sort_order"));
        item.setStatus(ItineraryItem.ItemStatus.valueOf(rs.getString("status")));
        long suggested = rs.getLong("suggested_by");
        if (!rs.wasNull()) item.setSuggestedBy(suggested);
        return item;
    }
}
