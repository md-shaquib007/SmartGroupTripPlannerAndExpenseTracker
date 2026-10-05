package com.tripsync.servlet;

import com.tripsync.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    protected Long parseLongParam(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric parameter: " + value);
        }
    }

    protected void handleError(HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof IllegalArgumentException) {
            JsonUtil.writeError(resp, 400, e.getMessage());
        } else if (e instanceof SecurityException) {
            JsonUtil.writeError(resp, 403, e.getMessage());
        } else if (e instanceof IllegalStateException) {
            JsonUtil.writeError(resp, 409, e.getMessage());
        } else {
            log.error("Unhandled servlet error", e);
            JsonUtil.writeError(resp, 500, "Internal server error");
        }
    }
}

