package com.eauction.dao;

import com.eauction.model.Notification;
import com.eauction.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationDAO {

    private static final Logger LOG = Logger.getLogger(NotificationDAO.class.getName());

    // ── Write ──────────────────────────────────────────────────────────────

    public boolean createNotification(int userId, Integer productId, String type, String message) {
        final String sql =
            "INSERT INTO notifications (user_id, product_id, type, message) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (productId != null) ps.setInt(2, productId); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, type);
            ps.setString(4, message);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "createNotification() SQL error", e);
            return false;
        }
    }

    public void markAllRead(int userId) {
        final String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "markAllRead() SQL error", e);
        }
    }

    public void markRead(int notifId, int userId) {
        final String sql =
            "UPDATE notifications SET is_read = TRUE WHERE notif_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "markRead() SQL error", e);
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public List<Notification> getRecentNotifications(int userId, int limit) {
        final String sql =
            "SELECT n.*, p.product_name "
            + "FROM notifications n "
            + "LEFT JOIN products p ON n.product_id = p.product_id "
            + "WHERE n.user_id = ? "
            + "ORDER BY n.created_at DESC LIMIT ?";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getRecentNotifications() SQL error", e);
        }
        return list;
    }

    public int getUnreadCount(int userId) {
        final String sql =
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "getUnreadCount() SQL error", e);
        }
        return 0;
    }

    // ── Mapping ────────────────────────────────────────────────────────────

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotifId(rs.getInt("notif_id"));
        n.setUserId(rs.getInt("user_id"));
        int pid = rs.getInt("product_id");
        if (!rs.wasNull()) n.setProductId(pid);
        n.setType(rs.getString("type"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));
        // product_name is nullable (LEFT JOIN)
        String pname = rs.getString("product_name");
        if (pname != null) n.setProductName(pname);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        return n;
    }
}
