package com.eauction.dao;

import com.eauction.model.User;
import com.eauction.util.DBConnection;
import com.eauction.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {

    private static final Logger LOG = Logger.getLogger(UserDAO.class.getName());

    // ── Authentication ─────────────────────────────────────────────────────

    /**
     * Verifies credentials and returns the User if valid and active.
     * Uses BCrypt for password comparison, NOT a plain DB equality check,
     * so SQL injection via the password field is impossible.
     */
    public User login(String email, String password) {
        final String sql =
            "SELECT * FROM users WHERE email = ? AND is_active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (PasswordUtil.verifyPassword(password, storedHash)) {
                        return mapUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "login() SQL error for email=" + email, e);
        }
        return null;
    }

    // ── Registration ───────────────────────────────────────────────────────

    public boolean register(User user) {
        final String sql =
            "INSERT INTO users (name, email, password, phone, address, role) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getName().trim());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, PasswordUtil.hashPassword(user.getPassword()));
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getAddress());
            ps.setString(6, user.getRole());
            return ps.executeUpdate() > 0;

        } catch (SQLIntegrityConstraintViolationException e) {
            // duplicate e-mail – caller can check emailExists() first
            return false;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "register() SQL error", e);
            return false;
        }
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public boolean emailExists(String email) {
        final String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "emailExists() SQL error", e);
        }
        return false;
    }

    public User getUserById(int userId) {
        final String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getUserById() SQL error, id=" + userId, e);
        }
        return null;
    }

    public List<User> getAllUsers() {
        final String sql =
            "SELECT * FROM users ORDER BY registration_date DESC";
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapUser(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getAllUsers() SQL error", e);
        }
        return users;
    }

    public int getTotalUsers() {
        final String sql = "SELECT COUNT(*) FROM users WHERE role != 'admin'";
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "getTotalUsers() SQL error", e);
        }
        return 0;
    }

    // ── Admin actions ──────────────────────────────────────────────────────

    /** Toggles is_active for a non-admin user.  Returns false on error. */
    public boolean toggleUserStatus(int userId) {
        final String sql =
            "UPDATE users SET is_active = NOT is_active "
            + "WHERE user_id = ? AND role != 'admin'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "toggleUserStatus() SQL error, id=" + userId, e);
        }
        return false;
    }

    // ── Profile update ─────────────────────────────────────────────────────

    /**
     * Updates name, phone, and address for the given user.
     * Returns the refreshed User object (or null on failure).
     */
    public User updateProfile(int userId, String name, String phone, String address) {
        final String sql =
            "UPDATE users SET name = ?, phone = ?, address = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, phone == null ? "" : phone.trim());
            ps.setString(3, address == null ? "" : address.trim());
            ps.setInt(4, userId);
            if (ps.executeUpdate() > 0) {
                return getUserById(userId);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "updateProfile() SQL error, id=" + userId, e);
        }
        return null;
    }

    // ── Change password ────────────────────────────────────────────────────

    /**
     * Changes the password after verifying the current one.
     *
     * @return "SUCCESS" | "WRONG_CURRENT" | "ERROR"
     */
    public String changePassword(int userId, String currentPassword, String newPassword) {
        // 1. Load current hash
        final String fetchSql = "SELECT password FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement fetchPs = conn.prepareStatement(fetchSql)) {

            fetchPs.setInt(1, userId);
            try (ResultSet rs = fetchPs.executeQuery()) {
                if (!rs.next()) return "ERROR";
                String storedHash = rs.getString("password");
                if (!PasswordUtil.verifyPassword(currentPassword, storedHash)) {
                    return "WRONG_CURRENT";
                }
            }

            // 2. Write new hash
            final String updateSql =
                "UPDATE users SET password = ? WHERE user_id = ?";
            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setString(1, PasswordUtil.hashPassword(newPassword));
                updatePs.setInt(2, userId);
                return updatePs.executeUpdate() > 0 ? "SUCCESS" : "ERROR";
            }

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "changePassword() SQL error, id=" + userId, e);
            return "ERROR";
        }
    }

    // ── Mapping ────────────────────────────────────────────────────────────

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));   // hash only
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("registration_date");
        if (ts != null) u.setRegistrationDate(ts.toLocalDateTime());
        return u;
    }
}
