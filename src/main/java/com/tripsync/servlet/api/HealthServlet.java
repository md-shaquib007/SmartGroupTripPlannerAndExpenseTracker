package com.tripsync.servlet.api;

import com.tripsync.util.JdbcUtil;
import com.tripsync.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/health")
public class HealthServlet extends HttpServlet {

    private static final long START_TIME = System.currentTimeMillis();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("uptimeMs", System.currentTimeMillis() - START_TIME);

        Map<String, Object> details = new HashMap<>();

        // Database Liveness Probe
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                details.put("database", Map.of("status", "UP", "database", conn.getMetaData().getDatabaseProductName()));
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            details.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        // JVM Memory Info
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        details.put("memory", Map.of(
                "totalMB", totalMemory / (1024 * 1024),
                "freeMB", freeMemory / (1024 * 1024),
                "usedMB", (totalMemory - freeMemory) / (1024 * 1024),
                "maxMB", maxMemory / (1024 * 1024)
        ));

        health.put("components", details);

        int statusCode = "UP".equals(health.get("status")) ? 200 : 503;
        JsonUtil.writeJson(resp, statusCode, health);
    }
}
