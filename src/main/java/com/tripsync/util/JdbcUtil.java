package com.tripsync.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public final class JdbcUtil {

    private static final HikariDataSource dataSource;

    static {
        try {
            Properties props = new Properties();
            try (InputStream in = JdbcUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                }
            } catch (Exception ignored) {}

            String envUrl = System.getenv("neonDbUrl") != null ? System.getenv("neonDbUrl")
                    : (System.getenv("NEON_DB_URL") != null ? System.getenv("NEON_DB_URL")
                    : (System.getenv("dbUrl") != null ? System.getenv("dbUrl") : System.getenv("DB_URL")));
            String envHost = System.getenv("dbHost") != null ? System.getenv("dbHost") : System.getenv("DB_HOST");
            String envPort = System.getenv("dbPort") != null ? System.getenv("dbPort") : System.getenv("DB_PORT");
            String envName = System.getenv("dbName") != null ? System.getenv("dbName") : System.getenv("DB_NAME");

            String url;
            if (envUrl != null && !envUrl.isBlank()) {
                url = envUrl;
            } else if (envHost != null && !envHost.isBlank()) {
                String host = envHost;
                String port = (envPort != null && !envPort.isBlank()) ? envPort : "3306";
                String name = (envName != null && !envName.isBlank()) ? envName : "tripsync";
                url = "jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false&allowPublicKeyRetrieval=true";
            } else {
                url = props.getProperty("neonDbUrl", props.getProperty("dbUrl", props.getProperty("db.url", "jdbc:mysql://localhost:3306/tripsync?useSSL=false&allowPublicKeyRetrieval=true")));
            }

            String username = System.getenv("dbUsername") != null ? System.getenv("dbUsername")
                    : (System.getenv("DB_USER") != null ? System.getenv("DB_USER")
                    : props.getProperty("dbUsername", props.getProperty("db.username", "root")));
            String password = System.getenv("dbPassword") != null ? System.getenv("dbPassword")
                    : (System.getenv("DB_PASS") != null ? System.getenv("DB_PASS")
                    : props.getProperty("dbPassword", props.getProperty("db.password", "root")));

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(url.startsWith("jdbc:h2:") ? "org.h2.Driver" : "com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);
            config.setLeakDetectionThreshold(15000);

            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private JdbcUtil() {}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static <T> T queryOne(String sql, ResultSetMapper<T> mapper, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapper.map(rs);
                }
                return null;
            }
        }
    }

    public static long insert(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            setParams(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert did not return generated key");
            }
        }
    }

    public static int update(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        }
    }

    public static int delete(String sql, Object... params) throws SQLException {
        return update(sql, params);
    }

    private static void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
