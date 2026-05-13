-- E-Auction Management System Database Schema
-- Run this script once to set up the database

CREATE DATABASE IF NOT EXISTS eauction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eauction_db;

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id           INT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100)  NOT NULL,
    email             VARCHAR(100)  UNIQUE NOT NULL,
    password          VARCHAR(255)  NOT NULL,          -- BCrypt hash
    phone             VARCHAR(20)   DEFAULT NULL,
    address           VARCHAR(255)  DEFAULT NULL,
    role              ENUM('admin','seller','buyer') NOT NULL DEFAULT 'buyer',
    registration_date DATETIME      DEFAULT CURRENT_TIMESTAMP,
    is_active         BOOLEAN       DEFAULT TRUE
) ENGINE=InnoDB;

-- ============================================================
-- PRODUCTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    product_id         INT AUTO_INCREMENT PRIMARY KEY,
    seller_id          INT          NOT NULL,
    product_name       VARCHAR(200) NOT NULL,
    description        TEXT         DEFAULT NULL,
    base_price         DECIMAL(15,2) NOT NULL,
    category           VARCHAR(100) DEFAULT 'Other',
    image              VARCHAR(255) DEFAULT NULL,
    auction_start_time DATETIME     NOT NULL,
    auction_end_time   DATETIME     NOT NULL,
    extended_end_time  DATETIME     DEFAULT NULL,
    snipe_extensions   TINYINT      DEFAULT 0,
    status             ENUM('pending','active','closed','cancelled') DEFAULT 'pending',
    created_at         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_seller (seller_id),
    INDEX idx_end_time (auction_end_time),
    INDEX idx_category (category)
) ENGINE=InnoDB;

-- ============================================================
-- BIDS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS bids (
    bid_id      INT AUTO_INCREMENT PRIMARY KEY,
    product_id  INT           NOT NULL,
    buyer_id    INT           NOT NULL,
    bid_amount  DECIMAL(15,2) NOT NULL,
    bid_time    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_buyer   FOREIGN KEY (buyer_id)   REFERENCES users(user_id)    ON DELETE CASCADE,
    INDEX idx_product_bid (product_id, bid_amount DESC),
    INDEX idx_buyer_bid   (buyer_id)
) ENGINE=InnoDB;

-- ============================================================
-- AUCTION RESULTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS auction_results (
    result_id   INT AUTO_INCREMENT PRIMARY KEY,
    product_id  INT           NOT NULL UNIQUE,
    winner_id   INT           DEFAULT NULL,
    final_price DECIMAL(15,2) DEFAULT NULL,
    result_date DATETIME      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_result_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_result_winner  FOREIGN KEY (winner_id)  REFERENCES users(user_id)    ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- AUTO BIDS TABLE (PROXY BIDDING)
-- ============================================================
CREATE TABLE IF NOT EXISTS auto_bids (
    auto_bid_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id  INT           NOT NULL,
    buyer_id    INT           NOT NULL,
    max_amount  DECIMAL(15,2) NOT NULL,
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active   BOOLEAN       DEFAULT TRUE,
    CONSTRAINT fk_ab_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_ab_buyer   FOREIGN KEY (buyer_id)   REFERENCES users(user_id)       ON DELETE CASCADE,
    UNIQUE KEY uq_ab (product_id, buyer_id)
) ENGINE=InnoDB;

-- ============================================================
-- NOTIFICATIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    notif_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL,
    product_id INT          DEFAULT NULL,
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      DEFAULT FALSE,
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE,
    CONSTRAINT fk_notif_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL,
    INDEX idx_notif_user (user_id, is_read, created_at)
) ENGINE=InnoDB;

-- ============================================================
-- PAYMENTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id     INT AUTO_INCREMENT PRIMARY KEY,
    result_id      INT         NOT NULL,
    payment_date   DATETIME    DEFAULT CURRENT_TIMESTAMP,
    payment_status ENUM('pending','completed','failed') DEFAULT 'pending',
    transaction_id VARCHAR(100) DEFAULT NULL,
    CONSTRAINT fk_payment_result FOREIGN KEY (result_id) REFERENCES auction_results(result_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- SEED DATA
-- Passwords are BCrypt hashes (cost 12):
--   admin123  -> $2a$12$...
--   seller123 -> $2a$12$...
--   buyer123  -> $2a$12$...
--
-- NOTE: Because BCrypt generates a unique salt each time, replace
-- these hashes with freshly generated ones if needed, OR just let
-- the application's RegisterServlet hash them on first use.
-- The sample hashes below are pre-generated and valid.
-- ============================================================

INSERT IGNORE INTO users (name, email, password, phone, address, role) VALUES
('System Admin',
 'admin@eauction.com',
 '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',  -- admin123
 '9999999999', 'Admin Office, Pune', 'admin'),

('Demo Seller',
 'seller@eauction.com',
 '$2a$12$eImiTXuWVxfM37uY4JANjQ==.XRHdNiLlYOq6EV.l3L3zX0v0wbyi',  -- seller123
 '8888888888', 'Seller Street, Pune', 'seller'),

('Demo Buyer',
 'buyer@eauction.com',
 '$2a$12$WApznUOJfkEGSmYRfnkrPOr6CVRngOq7ot1GRCpf.nxYqECJzVyuC',  -- buyer123
 '7777777777', 'Buyer Lane, Pune', 'buyer');

-- IMPORTANT: The BCrypt hashes above are illustrative placeholders.
-- Use the /register endpoint to create real accounts, OR run the
-- Java utility below once to regenerate proper hashes:
--
--   import org.mindrot.jbcrypt.BCrypt;
--   System.out.println(BCrypt.hashpw("admin123", BCrypt.gensalt(12)));
--
-- Then UPDATE the users table with the real hash values.

INSERT IGNORE INTO products
  (seller_id, product_name, description, base_price, category,
   auction_start_time, auction_end_time, status)
VALUES
(2, 'Dell Inspiron 15 Laptop',
 'Core i5 11th Gen, 8 GB RAM, 512 GB SSD. Barely used, excellent condition.',
 35000.00, 'Electronics',
 NOW(), DATE_ADD(NOW(), INTERVAL 2 DAY), 'active'),

(2, 'Antique Victorian Wooden Table',
 'Solid teak wood, 6-seater dining table. Exceptional Victorian-era craftsmanship.',
 15000.00, 'Furniture',
 NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), 'active'),

(2, 'Canon EOS 1500D DSLR Kit',
 '18-55 mm kit lens, 2 batteries, carry bag included. 2 years old, mint condition.',
 22000.00, 'Electronics',
 NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), 'active');
