package com.eauction.dao;

import com.eauction.model.Product;
import com.eauction.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDAO {

    private static final Logger LOG = Logger.getLogger(ProductDAO.class.getName());

    private static final String CURRENT_BID_EXPR =
        "COALESCE((SELECT MAX(b.bid_amount) FROM bids b WHERE b.product_id = p.product_id), p.base_price)";

    private static final String BASE_SELECT =
        "SELECT p.*, u.name AS seller_name, " + CURRENT_BID_EXPR + " AS current_bid "
        + "FROM products p JOIN users u ON p.seller_id = u.user_id ";

    // ── Create ─────────────────────────────────────────────────────────────

    public boolean addProduct(Product product) {
        final String sql =
            "INSERT INTO products "
            + "(seller_id, product_name, description, base_price, category, image, "
            + " auction_start_time, auction_end_time, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, product.getSellerId());
            ps.setString(2, product.getProductName().trim());
            ps.setString(3, product.getDescription());
            ps.setDouble(4, product.getBasePrice());
            ps.setString(5, product.getCategory());
            ps.setString(6, product.getImage());
            ps.setTimestamp(7, Timestamp.valueOf(product.getAuctionStartTime()));
            ps.setTimestamp(8, Timestamp.valueOf(product.getAuctionEndTime()));
            ps.setString(9, product.getStatus() != null ? product.getStatus() : "pending");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "addProduct() SQL error", e);
            return false;
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public Product getProductById(int productId) {
        final String sql = BASE_SELECT + "WHERE p.product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProduct(rs);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getProductById() SQL error, id=" + productId, e);
        }
        return null;
    }

    public List<Product> getAllActiveProducts() {
        final String sql = BASE_SELECT
            + "WHERE p.status = 'active' AND p.auction_end_time > NOW() "
            + "ORDER BY p.auction_end_time ASC";
        return queryList(sql);
    }

    public List<Product> getProductsBySeller(int sellerId) {
        final String sql = BASE_SELECT + "WHERE p.seller_id = ? ORDER BY p.created_at DESC";
        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getProductsBySeller() SQL error", e);
        }
        return list;
    }

    public List<Product> getAllProducts() {
        final String sql = BASE_SELECT + "ORDER BY p.created_at DESC";
        return queryList(sql);
    }

    /**
     * Feature 10 – Search & Filter.
     * keyword  : matched against product_name with LIKE (null = no filter)
     * category : exact match on category column (null / "" = no filter)
     * sort     : "endTime" | "price" | "name"
     */
    public List<Product> searchProducts(String keyword, String category, String sort) {
        boolean hasKeyword  = keyword  != null && !keyword.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append("WHERE p.status = 'active' AND p.auction_end_time > NOW() ");
        if (hasKeyword)  sql.append("AND p.product_name LIKE ? ");
        if (hasCategory) sql.append("AND p.category = ? ");

        switch (sort == null ? "" : sort) {
            case "price": sql.append("ORDER BY current_bid ASC");   break;
            case "name":  sql.append("ORDER BY p.product_name ASC"); break;
            default:      sql.append("ORDER BY p.auction_end_time ASC");
        }

        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasKeyword)  ps.setString(idx++, "%" + keyword + "%");
            if (hasCategory) ps.setString(idx,   category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "searchProducts() SQL error", e);
        }
        return list;
    }

    // ── Stats ──────────────────────────────────────────────────────────────

    public int getActiveAuctionCount() {
        return countQuery("SELECT COUNT(*) FROM products WHERE status='active' AND auction_end_time > NOW()");
    }

    public double getTotalRevenue() {
        final String sql = "SELECT COALESCE(SUM(final_price),0) FROM auction_results";
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "getTotalRevenue() SQL error", e);
        }
        return 0;
    }

    // ── Update ─────────────────────────────────────────────────────────────

    public boolean updateProductStatus(int productId, String status) {
        final String sql = "UPDATE products SET status = ? WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "updateProductStatus() SQL error, id=" + productId, e);
            return false;
        }
    }

    // ── Auction auto-close ─────────────────────────────────────────────────

    public void processExpiredAuctions() {
        // Also respect extended_end_time when set
        final String sql =
            "SELECT product_id FROM products WHERE status = 'active' "
            + "AND COALESCE(extended_end_time, auction_end_time) <= NOW()";
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {
            while (rs.next()) closeAuction(rs.getInt("product_id"));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "processExpiredAuctions() SQL error", e);
        }
    }

    private void closeAuction(int productId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            final String checkSql = "SELECT 1 FROM auction_results WHERE product_id = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, productId);
                try (ResultSet checkRs = checkPs.executeQuery()) {
                    if (checkRs.next()) { conn.rollback(); return; }
                }
            }

            try (PreparedStatement closePs = conn.prepareStatement(
                    "UPDATE products SET status='closed' WHERE product_id = ?")) {
                closePs.setInt(1, productId);
                closePs.executeUpdate();
            }

            try (PreparedStatement winnerPs = conn.prepareStatement(
                    "SELECT buyer_id, bid_amount FROM bids "
                    + "WHERE product_id = ? ORDER BY bid_amount DESC LIMIT 1")) {
                winnerPs.setInt(1, productId);
                try (ResultSet winnerRs = winnerPs.executeQuery()) {
                    try (PreparedStatement resultPs = conn.prepareStatement(
                            "INSERT INTO auction_results (product_id, winner_id, final_price) VALUES (?,?,?)")) {
                        resultPs.setInt(1, productId);
                        if (winnerRs.next()) {
                            resultPs.setInt(2,    winnerRs.getInt("buyer_id"));
                            resultPs.setDouble(3, winnerRs.getDouble("bid_amount"));
                        } else {
                            resultPs.setNull(2, Types.INTEGER);
                            resultPs.setNull(3, Types.DECIMAL);
                        }
                        resultPs.executeUpdate();
                    }
                }
            }

            conn.commit();
            LOG.info("Auction closed: product_id=" + productId);

        } catch (SQLException e) {
            DBConnection.rollback(conn);
            LOG.log(Level.SEVERE, "closeAuction() SQL error, pid=" + productId, e);
        } finally {
            DBConnection.close(conn);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private List<Product> queryList(String sql) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapProduct(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "queryList() SQL error", e);
        }
        return list;
    }

    private int countQuery(String sql) {
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "countQuery() SQL error", e);
        }
        return 0;
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setSellerId(rs.getInt("seller_id"));
        p.setSellerName(rs.getString("seller_name"));
        p.setProductName(rs.getString("product_name"));
        p.setDescription(rs.getString("description"));
        p.setBasePrice(rs.getDouble("base_price"));
        p.setCurrentBid(rs.getDouble("current_bid"));
        p.setCategory(rs.getString("category"));
        p.setImage(rs.getString("image"));
        p.setStatus(rs.getString("status"));
        Timestamp start   = rs.getTimestamp("auction_start_time");
        Timestamp end     = rs.getTimestamp("auction_end_time");
        Timestamp created = rs.getTimestamp("created_at");
        if (start   != null) p.setAuctionStartTime(start.toLocalDateTime());
        if (created != null) p.setCreatedAt(created.toLocalDateTime());
        // Use extended_end_time when present (anti-snipe)
        try {
            Timestamp extEnd = rs.getTimestamp("extended_end_time");
            if (extEnd != null) { p.setAuctionEndTime(extEnd.toLocalDateTime()); }
            else if (end != null) { p.setAuctionEndTime(end.toLocalDateTime()); }
        } catch (SQLException ignored) {
            if (end != null) p.setAuctionEndTime(end.toLocalDateTime());
        }
        return p;
    }
}
