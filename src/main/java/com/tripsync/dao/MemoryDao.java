package com.tripsync.dao;

import com.tripsync.model.MediaFile;
import com.tripsync.model.Memory;
import com.tripsync.util.JdbcUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MemoryDao {

    public List<Memory> findByTripId(long tripId, Long currentUserId) throws SQLException {
        List<Memory> memories = new ArrayList<>();
        String sql = """
                SELECT m.*, u.name AS user_name,
                       (SELECT COUNT(*) FROM likes l WHERE l.memory_id = m.id) AS like_count,
                       (SELECT COUNT(*) FROM comments c WHERE c.memory_id = m.id) AS comment_count
                FROM memories m
                JOIN users u ON u.id = m.user_id
                WHERE m.trip_id = ?
                ORDER BY m.memory_date DESC, m.created_at DESC
                """;
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Memory memory = mapMemory(rs);
                    memory.setMediaFiles(findMedia(memory.getId()));
                    if (currentUserId != null) {
                        memory.setLikedByCurrentUser(hasLiked(memory.getId(), currentUserId));
                    }
                    memories.add(memory);
                }
            }
        }
        return memories;
    }

    public long create(Memory memory) throws SQLException {
        return JdbcUtil.insert(
                "INSERT INTO memories (trip_id, user_id, caption, memory_date, location) VALUES (?, ?, ?, ?, ?)",
                memory.getTripId(), memory.getUserId(), memory.getCaption(),
                memory.getMemoryDate() != null ? Date.valueOf(memory.getMemoryDate()) : null,
                memory.getLocation()
        );
    }

    public void addMedia(long memoryId, String fileUrl, MediaFile.FileType type, String fileName) throws SQLException {
        JdbcUtil.insert(
                "INSERT INTO media_files (memory_id, file_url, file_type, file_name) VALUES (?, ?, ?, ?)",
                memoryId, fileUrl, type.name(), fileName
        );
    }

    public void toggleLike(long memoryId, long userId) throws SQLException {
        boolean liked = hasLiked(memoryId, userId);
        if (liked) {
            JdbcUtil.delete("DELETE FROM likes WHERE memory_id = ? AND user_id = ?", memoryId, userId);
        } else {
            JdbcUtil.insert("INSERT INTO likes (memory_id, user_id) VALUES (?, ?)", memoryId, userId);
        }
    }

    private boolean hasLiked(long memoryId, long userId) throws SQLException {
        Integer count = JdbcUtil.queryOne(
                "SELECT COUNT(*) AS cnt FROM likes WHERE memory_id = ? AND user_id = ?",
                rs -> rs.getInt("cnt"),
                memoryId, userId
        );
        return count != null && count > 0;
    }

    private List<MediaFile> findMedia(long memoryId) throws SQLException {
        List<MediaFile> files = new ArrayList<>();
        String sql = "SELECT * FROM media_files WHERE memory_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MediaFile f = new MediaFile();
                    f.setId(rs.getLong("id"));
                    f.setMemoryId(rs.getLong("memory_id"));
                    f.setFileUrl(rs.getString("file_url"));
                    f.setFileType(MediaFile.FileType.valueOf(rs.getString("file_type")));
                    f.setFileName(rs.getString("file_name"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) f.setCreatedAt(created.toLocalDateTime());
                    files.add(f);
                }
            }
        }
        return files;
    }

    private Memory mapMemory(ResultSet rs) throws SQLException {
        Memory m = new Memory();
        m.setId(rs.getLong("id"));
        m.setTripId(rs.getLong("trip_id"));
        m.setUserId(rs.getLong("user_id"));
        m.setUserName(rs.getString("user_name"));
        m.setCaption(rs.getString("caption"));
        Date d = rs.getDate("memory_date");
        if (d != null) m.setMemoryDate(d.toLocalDate());
        m.setLocation(rs.getString("location"));
        m.setLikeCount(rs.getInt("like_count"));
        m.setCommentCount(rs.getInt("comment_count"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) m.setCreatedAt(created.toLocalDateTime());
        return m;
    }
}
