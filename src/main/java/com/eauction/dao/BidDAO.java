package com.eauction.dao;

import com.eauction.model.Bid;
import com.eauction.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BidDAO {

    private static final Logger LOG = Logger.getLogger(BidDAO.class.getName());

    /**
     * Places a bid inside a single serialised transaction with SELECT…FOR UPDATE
     * to prevent race conditions.
     *
     * @return "SUCCESS" or a human-readable error message
     */
    public String placeBid(Bid bid) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // ── 1. Lock the product row ────────────────────────────────────
            final String lockSql =
                "SELECT status, auction_end_time, base_price, seller_id "
                + "FROM products WHERE product_id = ? FOR UPDATE";
            try (PreparedStatement lockPs = conn.prepareStatement(lockSql)) {
                lockPs.setInt(1, bid.getProductId());
                try (ResultSet lockRs = lockPs.executeQuery()) {
                    if (!lockRs.next()) {
                        DBConnection.rollback(conn);
                        return "Product not found.";
                    }

                    String status  = lockRs.getString("status");
                    Timestamp end  = lockRs.getTimestamp("auction_end_time");
                    double base    = lockRs.getDouble("base_price");
                    int    sellerId= lockRs.getInt("seller_id");

                    if (!"active".equals(status)) {
                        DBConnection.rollback(conn);
                        return "This auction is not active.";
                    }
                    if (end == null || end.before(new Timestamp(System.currentTimeMillis()))) {
                        DBConnection.rollback(conn);
                        return "This auction has already ended.";
                    }
                    if (sellerId == bid.getBuyerId()) {
                        DBConnection.rollback(conn);
                        return "Sellers cannot bid on their own products.";
                    }

                    // ── 2. Check current highest bid ───────────────────────
                    final String maxSql =
                        "SELECT COALESCE(MAX(bid_amount), ?) AS max_bid "
                        + "FROM bids WHERE product_id = ?";
                    try (PreparedStatement maxPs = conn.prepareStatement(maxSql)) {
                        maxPs.setDouble(1, base);
                        maxPs.setInt(2, bid.getProductId());
                        try (ResultSet maxRs = maxPs.executeQuery()) {
                            double currentMax = base;
                            if (maxRs.next()) currentMax = maxRs.getDouble("max_bid");

                            if (bid.getBidAmount() <= currentMax) {
                                DBConnection.rollback(conn);
                                return "Your bid must be higher than the current bid of "
                                    + String.format("₹%,.2f", currentMax) + ".";
                            }
                        }
                    }
                }
            }

            // ── 3. Insert the new bid ──────────────────────────────────────
            final String insertSql =
                "INSERT INTO bids (product_id, buyer_id, bid_amount) VALUES (?, ?, ?)";
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setInt(1, bid.getProductId());
                insertPs.setInt(2, bid.getBuyerId());
                insertPs.setDouble(3, bid.getBidAmount());
                insertPs.executeUpdate();
            }

            conn.commit();
            return "SUCCESS";

        } catch (SQLException e) {
            DBConnection.rollback(conn);
            LOG.log(Level.SEVERE, "placeBid() SQL error", e);
            return "A database error occurred. Please try again.";
        } finally {
            DBConnection.close(conn);
        }
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public List<Bid> getBidsByProduct(int productId) {
        final String sql =
            "SELECT b.*, u.name AS buyer_name "
            + "FROM bids b JOIN users u ON b.buyer_id = u.user_id "
            + "WHERE b.product_id = ? "
            + "ORDER BY b.bid_amount DESC";
        List<Bid> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBid(rs, false));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getBidsByProduct() SQL error, pid=" + productId, e);
        }
        return list;
    }

    public List<Bid> getBidsByBuyer(int buyerId) {
        final String sql =
            "SELECT b.*, u.name AS buyer_name, p.product_name "
            + "FROM bids b "
            + "JOIN users u    ON b.buyer_id   = u.user_id "
            + "JOIN products p ON b.product_id = p.product_id "
            + "WHERE b.buyer_id = ? "
            + "ORDER BY b.bid_time DESC";
        List<Bid> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bid b = mapBid(rs, false);
                    b.setProductName(rs.getString("product_name"));
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getBidsByBuyer() SQL error, uid=" + buyerId, e);
        }
        return list;
    }

    public double getHighestBid(int productId) {
        final String sql =
            "SELECT COALESCE(MAX(bid_amount), 0) FROM bids WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "getHighestBid() SQL error, pid=" + productId, e);
        }
        return 0;
    }

    // ── Mapping ────────────────────────────────────────────────────────────

    private Bid mapBid(ResultSet rs, boolean includeProductName) throws SQLException {
        Bid b = new Bid();
        b.setBidId(rs.getInt("bid_id"));
        b.setProductId(rs.getInt("product_id"));
        b.setBuyerId(rs.getInt("buyer_id"));
        b.setBuyerName(rs.getString("buyer_name"));
        b.setBidAmount(rs.getDouble("bid_amount"));
        Timestamp ts = rs.getTimestamp("bid_time");
        if (ts != null) b.setBidTime(ts.toLocalDateTime());
        return b;
    }
}
