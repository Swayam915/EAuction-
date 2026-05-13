-- E-Auction Management System Database Schema (H2 Compatible)
-- Run this script once to set up the database

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
    role              VARCHAR(20)   NOT NULL DEFAULT 'buyer' CHECK (role IN ('admin','seller','buyer')),
    registration_date TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    is_active         BOOLEAN       DEFAULT TRUE
);

-- ============================================================
-- PRODUCTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    product_id         INT AUTO_INCREMENT PRIMARY KEY,
    seller_id          INT          NOT NULL,
    product_name       VARCHAR(200) NOT NULL,
    description        CLOB         DEFAULT NULL,
    base_price         DECIMAL(15,2) NOT NULL,
    category           VARCHAR(100) DEFAULT 'Other',
    image              VARCHAR(255) DEFAULT NULL,
    auction_start_time TIMESTAMP    NOT NULL,
    auction_end_time   TIMESTAMP    NOT NULL,
    status             VARCHAR(20)  DEFAULT 'pending' CHECK (status IN ('pending','active','closed','cancelled')),
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_seller ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_end_time ON products(auction_end_time);

-- ============================================================
-- BIDS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS bids (
    bid_id      INT AUTO_INCREMENT PRIMARY KEY,
    product_id  INT           NOT NULL,
    buyer_id    INT           NOT NULL,
    bid_amount  DECIMAL(15,2) NOT NULL,
    bid_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_buyer   FOREIGN KEY (buyer_id)   REFERENCES users(user_id)    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_bid ON bids(product_id, bid_amount DESC);
CREATE INDEX IF NOT EXISTS idx_buyer_bid ON bids(buyer_id);

-- ============================================================
-- AUCTION RESULTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS auction_results (
    result_id   INT AUTO_INCREMENT PRIMARY KEY,
    product_id  INT           NOT NULL UNIQUE,
    winner_id   INT           DEFAULT NULL,
    final_price DECIMAL(15,2) DEFAULT NULL,
    result_date TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_result_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_result_winner  FOREIGN KEY (winner_id)  REFERENCES users(user_id)    ON DELETE SET NULL
);

-- ============================================================
-- PAYMENTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id     INT AUTO_INCREMENT PRIMARY KEY,
    result_id      INT         NOT NULL,
    payment_date   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    payment_status VARCHAR(20) DEFAULT 'pending' CHECK (payment_status IN ('pending','completed','failed')),
    transaction_id VARCHAR(100) DEFAULT NULL,
    CONSTRAINT fk_payment_result FOREIGN KEY (result_id) REFERENCES auction_results(result_id) ON DELETE CASCADE
);

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

INSERT INTO users (name, email, password, phone, address, role) VALUES
('System Admin',
 'admin@eauction.com',
 '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',  -- admin123
 '9999999999', 'Admin Office, Pune', 'admin'),

('John Seller',
 'seller@eauction.com',
 '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',  -- seller123
 '8888888888', 'Seller Office, Mumbai', 'seller'),

('Jane Buyer',
 'buyer@eauction.com',
 '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',  -- buyer123
 '7777777777', 'Buyer Home, Delhi', 'buyer');