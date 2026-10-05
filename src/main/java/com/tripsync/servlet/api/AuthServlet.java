package com.tripsync.servlet.api;

import com.google.gson.JsonObject;
import com.tripsync.filter.AuthFilter;
import com.tripsync.model.User;
import com.tripsync.service.AuthService;
import com.tripsync.servlet.BaseServlet;
import com.tripsync.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            JsonObject body = JsonUtil.parseBody(readBody(req));

            if ("/register".equals(path)) {
                User user = authService.register(
                        JsonUtil.getString(body, "name"),
                        JsonUtil.getString(body, "email"),
                        JsonUtil.getString(body, "password"),
                        JsonUtil.getString(body, "phone")
                );
                // Fix session fixation: invalidate any existing session before creating new one
                HttpSession old = req.getSession(false);
                if (old != null) old.invalidate();
                req.getSession(true).setAttribute("user", user);
                JsonUtil.writeJson(resp, 201, user);
            } else if ("/login".equals(path)) {
                User user = authService.login(
                        JsonUtil.getString(body, "email"),
                        JsonUtil.getString(body, "password")
                );
                // Fix session fixation: invalidate any existing session before creating new one
                HttpSession old = req.getSession(false);
                if (old != null) old.invalidate();
                req.getSession(true).setAttribute("user", user);
                JsonUtil.writeJson(resp, 200, user);
            } else if ("/forgot-password".equals(path)) {
                Map<String, String> result = authService.requestPasswordReset(JsonUtil.getString(body, "email"));
                JsonUtil.writeJson(resp, 200, result);
            } else if ("/reset-password".equals(path)) {
                authService.resetPassword(
                        JsonUtil.getString(body, "token"),
                        JsonUtil.getString(body, "password")
                );
                JsonUtil.writeJson(resp, 200, Map.of("message", "Password reset successful"));
            } else {
                JsonUtil.writeError(resp, 404, "Not found");
            }
        } catch (Exception e) {
            handleError(resp, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/me".equals(req.getPathInfo())) {
            User user = AuthFilter.currentUser(req);
            if (user == null) {
                JsonUtil.writeError(resp, 401, "Not authenticated");
            } else {
                JsonUtil.writeJson(resp, 200, user);
            }
            return;
        }
        JsonUtil.writeError(resp, 404, "Not found");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/logout".equals(req.getPathInfo())) {
            HttpSession session = req.getSession(false);
            if (session != null) session.invalidate();
            JsonUtil.writeJson(resp, 200, Map.of("message", "Logged out"));
        } else {
            JsonUtil.writeError(resp, 404, "Not found");
        }
    }
}
