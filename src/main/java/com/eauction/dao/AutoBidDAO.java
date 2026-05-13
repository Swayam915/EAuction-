package com.eauction.dao;

import com.eauction.model.AutoBid;
import com.eauction.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data-access for the auto_bids table.
 * All writes use INSERT … ON DUPLICATE KEY UPDATE so there is always
 * at most one active proxy-bid row per (product_id, buyer_id).
 */
public class AutoBidDAO {

    private static final Logger LOG = Logger.getLogger(AutoBidDAO.class.getName());

    // ── Upsert ─────────────────────────────────────────────────────────────

    /**
     * Creates or updates the buyer's max-bid for a product.
     * Returns true on success.
     */
    public boolean saveAutoBid(int productId, int buyerId, double maxAmount) {
        String updateSql = "UPDATE auto_bids SET max_amount = ?, is_active = TRUE, updated_at = CURRENT_TIMESTAMP WHERE product_id = ? AND buyer_id = ?";
        String insertSql = "INSERT INTO auto_bids (product_id, buyer_id, max_amount, is_active) VALUES (?, ?, ?, TRUE)";

        try (Connection conn = DBConnection.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int updated = 0;
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setDouble(1, maxAmount);
                    psUpdate.setInt(2, productId);
                    psUpdate.setInt(3, buyerId);
                    updated = psUpdate.executeUpdate();
                }

                if (updated == 0) {
                    try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                        psInsert.setInt(1, productId);
                        psInsert.setInt(2, buyerId);
                        psInsert.setDouble(3, maxAmount);
                        updated = psInsert.executeUpdate();
                    }
                }
                
                conn.commit();
                return updated > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "saveAutoBid() SQL error", e);
            return false;
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    /**
     * Returns all active proxy-bids for a product, ordered by max_amount DESC.
     * Used by the auto-bid engine.
     */
    public List<AutoBid> getActiveAutoBidsForProduct(int productId) {
        final String sql =
            "SELECT ab.*, u.name AS buyer_name "
            + "FROM auto_bids ab JOIN users u ON ab.buyer_id = u.user_id "
            + "WHERE ab.product_id = ? AND ab.is_active = TRUE "
            + "ORDER BY ab.max_amount DESC";
        List<AutoBid> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getActiveAutoBidsForProduct() SQL error", e);
        }
        return list;
    }

    /**
     * Returns the buyer's current proxy-bid for a product, or null if none.
     */
    public AutoBid getAutoBidForBuyer(int productId, int buyerId) {
        final String sql =
            "SELECT ab.*, u.name AS buyer_name "
            + "FROM auto_bids ab JOIN users u ON ab.buyer_id = u.user_id "
            + "WHERE ab.product_id = ? AND ab.buyer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getAutoBidForBuyer() SQL error", e);
        }
        return null;
    }

    /** Deactivates a buyer's proxy-bid (e.g. they were outbid beyond their limit). */
    public void deactivateAutoBid(int productId, int buyerId) {
        final String sql =
            "UPDATE auto_bids SET is_active = FALSE "
            + "WHERE product_id = ? AND buyer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, buyerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "deactivateAutoBid() SQL error", e);
        }
    }

    // ── Mapping ────────────────────────────────────────────────────────────

    private AutoBid map(ResultSet rs) throws SQLException {
        AutoBid ab = new AutoBid();
        ab.setAutoBidId(rs.getInt("auto_bid_id"));
        ab.setProductId(rs.getInt("product_id"));
        ab.setBuyerId(rs.getInt("buyer_id"));
        ab.setBuyerName(rs.getString("buyer_name"));
        ab.setMaxAmount(rs.getDouble("max_amount"));
        ab.setActive(rs.getBoolean("is_active"));
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ca != null) ab.setCreatedAt(ca.toLocalDateTime());
        if (ua != null) ab.setUpdatedAt(ua.toLocalDateTime());
        return ab;
    }
}
