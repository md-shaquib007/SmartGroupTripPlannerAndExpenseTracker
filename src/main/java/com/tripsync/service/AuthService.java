package com.tripsync.service;

import com.tripsync.dao.UserDao;
import com.tripsync.model.User;
import com.tripsync.util.PasswordUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public User register(String name, String email, String password, String phone) throws SQLException {
        if (userDao.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setPhone(phone);
        long id = userDao.create(user);
        user.setId(id);
        user.setPasswordHash(null);
        return user;
    }

    public User login(String email, String password) throws SQLException {
        User user = userDao.findByEmail(email);
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        user.setPasswordHash(null);
        return user;
    }

    public Map<String, String> requestPasswordReset(String email) throws SQLException {
        User user = userDao.findByEmail(email);
        if (user == null) {
            // Do NOT reveal whether email exists — return generic message to prevent user enumeration
            return Map.of("message", "If an account with that email exists, a reset link has been sent");
        }
        String token = PasswordUtil.generateToken();
        userDao.setResetToken(user.getId(), token, LocalDateTime.now().plusHours(1));
        // TODO: send token via email service (e.g. SendGrid/JavaMailSender)
        // emailService.sendPasswordResetEmail(user.getEmail(), token);
        return Map.of("message", "If an account with that email exists, a reset link has been sent");
    }

    public void resetPassword(String token, String newPassword) throws SQLException {
        User user = userDao.findByResetToken(token);
        if (user == null) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        userDao.updatePassword(user.getId(), PasswordUtil.hash(newPassword));
    }
}
