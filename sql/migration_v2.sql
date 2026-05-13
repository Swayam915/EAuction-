USE eauction_db;

-- 1. Auto-bids (works fine already)
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

-- 2. Notifications (works fine)
CREATE TABLE IF NOT EXISTS notifications (
    notif_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL,
    product_id INT          DEFAULT NULL,
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      DEFAULT FALSE,
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE,
    CONSTRAINT fk_notif_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ADD INDEX separately (safe way)
CREATE INDEX idx_notif_user ON notifications(user_id, is_read, created_at);

-- 3. Anti-snipe columns (NO IF NOT EXISTS)
ALTER TABLE products ADD COLUMN extended_end_time DATETIME DEFAULT NULL;
ALTER TABLE products ADD COLUMN snipe_extensions TINYINT DEFAULT 0;

-- 4. Indexes (NO IF NOT EXISTS)
CREATE INDEX idx_product_name ON products(product_name);
CREATE INDEX idx_product_category ON products(category, status);