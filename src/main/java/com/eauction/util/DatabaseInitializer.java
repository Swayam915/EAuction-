package com.eauction.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database initializer that runs the schema SQL on application startup.
 */
public final class DatabaseInitializer {

    private static final Logger LOG = Logger.getLogger(DatabaseInitializer.class.getName());

    private DatabaseInitializer() { /* utility class */ }

    /**
     * Initialize the database by running the schema SQL.
     */
    public static void initializeDatabase() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Execute schema statements in order
            executeStatement(stmt, "CREATE TABLE IF NOT EXISTS users (user_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, email VARCHAR(100) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL, phone VARCHAR(20) DEFAULT NULL, address VARCHAR(255) DEFAULT NULL, role VARCHAR(20) NOT NULL DEFAULT 'buyer' CHECK (role IN ('admin','seller','buyer')), registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, is_active BOOLEAN DEFAULT TRUE)");
            executeStatement(stmt, "CREATE TABLE IF NOT EXISTS products (product_id INT AUTO_INCREMENT PRIMARY KEY, seller_id INT NOT NULL, product_name VARCHAR(200) NOT NULL, description CLOB DEFAULT NULL, base_price DECIMAL(15,2) NOT NULL, category VARCHAR(100) DEFAULT 'Other', image VARCHAR(255) DEFAULT NULL, auction_start_time TIMESTAMP NOT NULL, auction_end_time TIMESTAMP NOT NULL, status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending','active','closed','cancelled')), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, extended_end_time TIMESTAMP DEFAULT NULL, snipe_extensions TINYINT DEFAULT 0, CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE)");
            executeStatement(stmt, "CREATE INDEX IF NOT EXISTS idx_status ON products(status)");
            executeStatement(stmt, "CREATE INDEX IF NOT EXISTS idx_seller ON products(seller_id)");
            executeStatement(stmt, "CREATE INDEX IF NOT EXISTS idx_end_time ON products(auction_end_time)");
            executeStatement(stmt, "CREATE TABLE IF NOT EXISTS bids (bid_id INT AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL, buyer_id INT NOT NULL, bid_amount DECIMAL(15,2) NOT NULL, bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_bid_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE, CONSTRAINT fk_bid_buyer FOREIGN KEY (buyer_id) REFERENCES users(user_id) ON DELETE CASCADE)");
            executeStatement(stmt, "CREATE INDEX IF NOT EXISTS idx_product_bid ON bids(product_id, bid_amount DESC)");
            executeStatement(stmt, "CREATE INDEX IF NOT EXISTS idx_buyer_bid ON bids(buyer_id)");
            executeStatement(stmt, "CREATE TABLE IF NOT EXISTS auction_results (result_id INT AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL UNIQUE, winner_id INT DEFAULT NULL, final_price DECIMAL(15,2) DEFAULT NULL, result_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_result_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE, CONSTRAINT fk_result_winner FOREIGN KEY (winner_id) REFERENCES users(user_id) ON DELETE SET NULL)");
            executeStatement(stmt, "CREATE TABLE IF NOT EXISTS payments (payment_id INT AUTO_INCREMENT PRIMARY KEY, result_id INT NOT NULL, payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, payment_status VARCHAR(20) DEFAULT 'pending' CHECK (payment_status IN ('pending','completed','failed')), transaction_id VARCHAR(100) DEFAULT NULL, CONSTRAINT fk_payment_result FOREIGN KEY (result_id) REFERENCES auction_results(result_id) ON DELETE CASCADE)");
            executeStatement(stmt, "CREATE TABLE IF NOT EXISTS auto_bids (auto_bid_id INT AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL, buyer_id INT NOT NULL, max_amount DECIMAL(15,2) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, is_active BOOLEAN DEFAULT TRUE, CONSTRAINT fk_ab_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE, CONSTRAINT fk_ab_buyer FOREIGN KEY (buyer_id) REFERENCES users(user_id) ON DELETE CASCADE)");
            executeStatement(stmt, "CREATE TABLE IF NOT EXISTS notifications (notif_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, product_id INT DEFAULT NULL, type VARCHAR(40) NOT NULL, message VARCHAR(500) NOT NULL, is_read BOOLEAN DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, CONSTRAINT fk_notif_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL)");
            executeStatement(stmt, "CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications(user_id, is_read, created_at)");
            executeStatement(stmt, "ALTER TABLE auto_bids ADD CONSTRAINT IF NOT EXISTS uq_ab UNIQUE (product_id, buyer_id)");

            // Upsert sample users with known secure passwords
            upsertUser(conn, "System Admin", "admin@eauction.com", "admin123", "9999999999", "Admin Office, Pune", "admin");
            upsertUser(conn, "John Seller", "seller@eauction.com", "seller123", "8888888888", "Seller Office, Mumbai", "seller");
            upsertUser(conn, "Jane Buyer", "buyer@eauction.com", "buyer123", "7777777777", "Buyer Home, Delhi", "buyer");

            // Upsert sample products for the seller so dashboards are not empty
            upsertSampleProduct(conn, "seller@eauction.com", "Apple iPhone 14", "Latest model, great condition", 52000.00, "Electronics", "active", 3);
            upsertSampleProduct(conn, "seller@eauction.com", "Oak Dining Table", "Solid wood, seats 6-8", 15000.00, "Furniture", "active", 5);

            LOG.info("Database initialization completed successfully");

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Database initialization failed", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private static void upsertUser(Connection conn, String name, String email, String password, String phone, String address, String role) throws Exception {
        String sql = "MERGE INTO users (name, email, password, phone, address, role, is_active) KEY(email) VALUES (?, ?, ?, ?, ?, ?, TRUE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email.toLowerCase());
            ps.setString(3, PasswordUtil.hashPassword(password));
            ps.setString(4, phone);
            ps.setString(5, address);
            ps.setString(6, role);
            ps.executeUpdate();
            LOG.info("Upserted user: " + email);
        }
    }

    private static void upsertSampleProduct(Connection conn, String sellerEmail, String productName, String description, double basePrice, String category, String status, int daysUntilEnd) throws Exception {
        String selectSeller = "SELECT user_id FROM users WHERE email = ?";
        int sellerId = -1;
        try (PreparedStatement ps = conn.prepareStatement(selectSeller)) {
            ps.setString(1, sellerEmail.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sellerId = rs.getInt("user_id");
                }
            }
        }
        if (sellerId <= 0) {
            LOG.warning("Seller not found for email: " + sellerEmail);
            return;
        }

        String productCheck = "SELECT 1 FROM products WHERE seller_id = ? AND product_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(productCheck)) {
            ps.setInt(1, sellerId);
            ps.setString(2, productName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LOG.info("Sample product already exists: " + productName);
                    return;
                }
            }
        }

        String insertSql = "INSERT INTO products (seller_id, product_name, description, base_price, category, image, auction_start_time, auction_end_time, status, snipe_extensions) VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.plusDays(daysUntilEnd);
            ps.setInt(1, sellerId);
            ps.setString(2, productName);
            ps.setString(3, description);
            ps.setDouble(4, basePrice);
            ps.setString(5, category);
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setTimestamp(7, Timestamp.valueOf(endTime));
            ps.setString(8, status);
            ps.executeUpdate();
            LOG.info("Inserted sample product: " + productName);
        }
    }

    private static void executeStatement(Statement stmt, String sql) throws Exception {
        try {
            stmt.execute(sql);
            LOG.info("Executed: " + sql.substring(0, Math.min(80, sql.length())) + (sql.length() > 80 ? "..." : ""));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to execute: " + sql.substring(0, Math.min(80, sql.length())), e);
            // Continue with other statements
        }
    }
}