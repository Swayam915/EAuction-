package com.eauction.service;

import com.eauction.dao.AutoBidDAO;
import com.eauction.dao.BidDAO;
import com.eauction.dao.NotificationDAO;
import com.eauction.dao.ProductDAO;
import com.eauction.model.AutoBid;
import com.eauction.model.BidIncrementRule;
import com.eauction.model.Notification;
import com.eauction.model.Product;
import com.eauction.util.BidStateCache;
import com.eauction.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ──────────────────────────────────────────────────────────────────────────
 * BidService — the single entry point for all bidding operations.
 *
 * Responsibilities
 *  1. Validate bid amount against current highest bid + minimum increment
 *  2. Anti-snipe: extend auction by 2 min if bid arrives in last 10 seconds
 *  3. Insert the manual bid
 *  4. Run the auto-bid engine: raise competing proxy bids if needed
 *  5. Create "outbid" notifications for displaced bidders
 *
 * All database work for a single user-bid is done inside ONE transaction
 * obtained from DBConnection.getConnection().
 * ──────────────────────────────────────────────────────────────────────────
 */
public class BidService {

    private static final Logger LOG = Logger.getLogger(BidService.class.getName());

    /** Minutes to extend auction on last-second snipe */
    private static final int  ANTI_SNIPE_EXTEND_MINUTES = 2;
    /** Window (seconds) before auction end that triggers anti-snipe */
    private static final int  ANTI_SNIPE_WINDOW_SECONDS = 10;
    /** Maximum number of anti-snipe extensions per auction */
    private static final int  MAX_EXTENSIONS = 5;

    private final BidDAO           bidDAO           = new BidDAO();
    private final AutoBidDAO       autoBidDAO       = new AutoBidDAO();
    private final NotificationDAO  notifDAO         = new NotificationDAO();
    private final ProductDAO       productDAO       = new ProductDAO();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Places a manual bid from buyerId on productId for bidAmount.
     *
     * @return "SUCCESS" or a human-readable error string
     */
    public String placeBid(int productId, int buyerId, double bidAmount) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // ── Step 1: Lock product row ───────────────────────────────────
            ProductSnapshot snap = lockAndLoadProduct(conn, productId);
            if (snap == null) { DBConnection.rollback(conn); return "Product not found."; }

            // ── Step 2: Validate status and time ──────────────────────────
            String statusError = validateAuctionOpen(snap, buyerId);
            if (statusError != null) { DBConnection.rollback(conn); return statusError; }

            // ── Step 3: Compute effective end time (may be extended) ───────
            LocalDateTime effectiveEnd = snap.extendedEnd != null ? snap.extendedEnd : snap.scheduledEnd;

            // ── Step 4: Validate bid amount ────────────────────────────────
            double currentBid = getCurrentBid(conn, productId, snap.basePrice);
            BidIncrementRule rule = BidIncrementRule.forCurrentBid(currentBid);
            double minBid = rule.getMinValidBid(currentBid);

            if (bidAmount < minBid) {
                DBConnection.rollback(conn);
                return String.format("Minimum bid is ₹%,.2f (current ₹%,.2f + increment ₹%,.2f).",
                    minBid, currentBid, rule.getMinIncrement());
            }

            // ── Step 5: Anti-snipe check ───────────────────────────────────
            long secondsLeft = java.time.Duration.between(LocalDateTime.now(), effectiveEnd).getSeconds();
            if (secondsLeft >= 0 && secondsLeft <= ANTI_SNIPE_WINDOW_SECONDS
                    && snap.extensions < MAX_EXTENSIONS) {
                extendAuction(conn, productId, snap.extensions);
            }

            // ── Step 6: Record who currently leads (to notify later) ───────
            int previousLeaderId = getPreviousLeader(conn, productId);

            // ── Step 6.5: Fraud Prevention (Duplicate Bid Detection) ───────
            if (previousLeaderId == buyerId) {
                DBConnection.rollback(conn);
                return "You are already the highest bidder.";
            }

            // ── Step 7: Insert the manual bid ─────────────────────────────
            insertBid(conn, productId, buyerId, bidAmount);

            // ── Step 8: Notify previous leader they were outbid ───────────
            if (previousLeaderId > 0 && previousLeaderId != buyerId) {
                notifyOutbid(previousLeaderId, productId, snap.productName, bidAmount);
            }

            // ── Step 9: Run auto-bid engine ────────────────────────────────
            runAutoBidEngine(conn, productId, buyerId, bidAmount, snap);

            // Update in-memory latest bid state for real-time sync.
            double finalTop = getCurrentBid(conn, productId, snap.basePrice);
            BidStateCache.setCurrentBid(productId, finalTop);

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

    /**
     * Saves (or updates) the buyer's auto-bid max amount.
     * Also immediately triggers the auto-bid engine so the buyer
     * is placed at the correct position right away.
     *
     * @return "SUCCESS" or error message
     */
    public String saveAutoBid(int productId, int buyerId, double maxAmount) {
        Product product = productDAO.getProductById(productId);
        if (product == null)         return "Product not found.";
        if (!product.isLive())       return "This auction is not active.";
        if (product.getSellerId() == buyerId) return "Sellers cannot proxy-bid on their own products.";

        double currentBid = bidDAO.getHighestBid(productId);
        if (currentBid == 0) currentBid = product.getBasePrice();

        if (maxAmount <= currentBid) {
            return String.format("Your maximum bid must be higher than the current bid of ₹%,.2f.", currentBid);
        }

        boolean saved = autoBidDAO.saveAutoBid(productId, buyerId, maxAmount);
        if (!saved) return "Failed to save auto-bid. Please try again.";

        // Trigger engine: if buyer is not already leading, place a bid at min increment
        double myBid = getCurrentLeadBid(productId, buyerId);
        if (myBid < currentBid + BidIncrementRule.forCurrentBid(currentBid).getMinIncrement()) {
            double targetBid = currentBid + BidIncrementRule.forCurrentBid(currentBid).getMinIncrement();
            if (targetBid <= maxAmount) {
                placeBid(productId, buyerId, targetBid);
            }
        }
        return "SUCCESS";
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private ProductSnapshot lockAndLoadProduct(Connection conn, int productId)
            throws SQLException {
        final String sql =
            "SELECT product_id, product_name, seller_id, base_price, status, "
            + "auction_end_time, extended_end_time, snipe_extensions "
            + "FROM products WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ProductSnapshot s = new ProductSnapshot();
                s.productName = rs.getString("product_name");
                s.sellerId    = rs.getInt("seller_id");
                s.basePrice   = rs.getDouble("base_price");
                s.status      = rs.getString("status");
                s.extensions  = rs.getInt("snipe_extensions");
                Timestamp end = rs.getTimestamp("auction_end_time");
                Timestamp ext = rs.getTimestamp("extended_end_time");
                if (end != null) s.scheduledEnd = end.toLocalDateTime();
                if (ext != null) s.extendedEnd  = ext.toLocalDateTime();
                return s;
            }
        }
    }

    private String validateAuctionOpen(ProductSnapshot snap, int buyerId) {
        if (!"active".equals(snap.status))    return "This auction is not active.";
        if (snap.sellerId == buyerId)          return "Sellers cannot bid on their own products.";
        LocalDateTime eff = snap.extendedEnd != null ? snap.extendedEnd : snap.scheduledEnd;
        if (eff == null || LocalDateTime.now().isAfter(eff)) return "This auction has already ended.";
        return null;
    }

    private double getCurrentBid(Connection conn, int productId, double basePrice)
            throws SQLException {
        final String sql =
            "SELECT COALESCE(MAX(bid_amount), ?) FROM bids WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, basePrice);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : basePrice;
            }
        }
    }

    private int getPreviousLeader(Connection conn, int productId) throws SQLException {
        final String sql =
            "SELECT buyer_id FROM bids WHERE product_id = ? ORDER BY bid_amount DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("buyer_id") : 0;
            }
        }
    }

    private void insertBid(Connection conn, int productId, int buyerId, double amount)
            throws SQLException {
        final String sql =
            "INSERT INTO bids (product_id, buyer_id, bid_amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, buyerId);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        }
    }

    private void extendAuction(Connection conn, int productId, int currentExtensions)
            throws SQLException {
        final String sql =
            "UPDATE products SET "
            + "  extended_end_time = COALESCE(extended_end_time, auction_end_time) "
            + "                      + INTERVAL " + ANTI_SNIPE_EXTEND_MINUTES + " MINUTE, "
            + "  snipe_extensions  = snipe_extensions + 1 "
            + "WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
        LOG.info("Anti-snipe: extended auction " + productId + " by "
            + ANTI_SNIPE_EXTEND_MINUTES + " minutes (extension #" + (currentExtensions + 1) + ")");
    }

    /**
     * Auto-bid engine: after a new manual bid arrives at newBidAmount by bidder,
     * look for competing proxy bids and raise one if it can beat the new bid.
     *
     * Algorithm:
     *  1. Find all active auto-bids sorted by max DESC.
     *  2. The top auto-bidder (excluding the current bidder) can respond if
     *     their maxAmount > newBidAmount.
     *  3. They bid exactly newBidAmount + minIncrement (or their max, whichever is lower)
     *     but only up to their limit.
     *  4. If multiple auto-bidders compete, they leapfrog until one runs out.
     */
    private void runAutoBidEngine(Connection conn, int productId,
                                  int humanBidderId, double humanBidAmount,
                                  ProductSnapshot snap) throws SQLException {
        List<AutoBid> autoBids = autoBidDAO.getActiveAutoBidsForProduct(productId);
        if (autoBids.isEmpty()) return;

        double currentTop     = humanBidAmount;
        int    currentLeader  = humanBidderId;

        for (AutoBid ab : autoBids) {
            if (ab.getBuyerId() == currentLeader) continue;  // they're already leading
            if (ab.getMaxAmount() <= currentTop)  continue;  // can't outbid

            // This auto-bidder can respond
            BidIncrementRule rule    = BidIncrementRule.forCurrentBid(currentTop);
            double           counter = Math.min(ab.getMaxAmount(), currentTop + rule.getMinIncrement());

            // Notify previous leader
            if (currentLeader != humanBidderId) {
                notifyOutbid(currentLeader, productId, snap.productName, counter);
            }

            insertBid(conn, productId, ab.getBuyerId(), counter);
            LOG.info("AutoBid engine: buyer " + ab.getBuyerId()
                + " placed ₹" + counter + " on product " + productId);

            currentTop    = counter;
            currentLeader = ab.getBuyerId();

            // If current human bidder was outbid, notify them
            if (currentLeader != humanBidderId) {
                notifyOutbid(humanBidderId, productId, snap.productName, counter);
            }

            // If this auto-bidder has reached their limit, deactivate them
            if (counter >= ab.getMaxAmount()) {
                autoBidDAO.deactivateAutoBid(productId, ab.getBuyerId());
            }

            // Only need one counter — the highest proxy-bidder wins the round
            break;
        }
    }

    private void notifyOutbid(int userId, int productId, String productName, double newBid) {
        String msg = String.format(
            "You were outbid on \"%s\". New highest bid: ₹%,.2f.", productName, newBid);
        notifDAO.createNotification(userId, productId, Notification.TYPE_OUTBID, msg);
    }

    private double getCurrentLeadBid(int productId, int buyerId) {
        final String sql =
            "SELECT COALESCE(MAX(bid_amount),0) FROM bids WHERE product_id=? AND buyer_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "getCurrentLeadBid() error", e);
            return 0;
        }
    }

    // ── Inner snapshot DTO (avoids a second DB query) ──────────────────────

    private static class ProductSnapshot {
        String        productName;
        int           sellerId;
        double        basePrice;
        String        status;
        LocalDateTime scheduledEnd;
        LocalDateTime extendedEnd;   // null if never extended
        int           extensions;
    }
}
