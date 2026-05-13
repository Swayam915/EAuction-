package com.eauction.service;

import com.eauction.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simulates concurrent bidding to verify thread-safety and real-time behavior.
 * This class fetches all mock buyers and continuously places bids on a target auction
 * using multiple threads.
 */
public class BidSimulator {

    private static final Logger LOG = Logger.getLogger(BidSimulator.class.getName());
    private final BidService bidService = new BidService();
    private volatile boolean running = false;
    private final List<Thread> activeThreads = new ArrayList<>();

    /**
     * Starts the multithreaded simulation for a given product.
     * @param productId The product to bid on
     * @param numThreads The number of concurrent threads to spawn
     * @param maxBidsPerThread The number of bids each thread should attempt
     */
    public void startSimulation(int productId, int numThreads, int maxBidsPerThread) {
        if (running) {
            LOG.warning("Simulation is already running!");
            return;
        }

        List<Integer> buyers = getActiveBuyers();
        if (buyers.isEmpty()) {
            LOG.warning("No active buyers found to simulate bids!");
            return;
        }

        running = true;
        activeThreads.clear();
        LOG.info("Starting simulation on Product " + productId + " with " + numThreads + " threads.");

        for (int i = 0; i < numThreads; i++) {
            Thread t = new BidThread(productId, buyers, maxBidsPerThread, i);
            activeThreads.add(t);
            t.start();
        }
    }

    public void stopSimulation() {
        running = false;
        LOG.info("Stopping simulation...");
        for (Thread t : activeThreads) {
            t.interrupt();
        }
    }

    private List<Integer> getActiveBuyers() {
        List<Integer> buyers = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE role = 'buyer'")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    buyers.add(rs.getInt("user_id"));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to fetch buyers for simulation", e);
        }
        return buyers;
    }

    /**
     * Inner class representing a single concurrent bidder.
     */
    private class BidThread extends Thread {
        private final int productId;
        private final List<Integer> availableBuyers;
        private final int maxBids;
        private final int threadId;
        private final Random random = new Random();

        public BidThread(int productId, List<Integer> buyers, int maxBids, int threadId) {
            this.productId = productId;
            this.availableBuyers = buyers;
            this.maxBids = maxBids;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            int bidsPlaced = 0;
            while (running && bidsPlaced < maxBids) {
                try {
                    // Pick a random buyer
                    int buyerId = availableBuyers.get(random.nextInt(availableBuyers.size()));

                    // We must fetch the current bid dynamically to know what to bid
                    double currentBid = getCurrentBid(productId);
                    double nextBid = currentBid + 1000.0; // Fixed increment for simulation
                    
                    // The synchronized keyword inside BidService via DB Locking ensures 
                    // two threads can't successfully bid the same amount.
                    String result = bidService.placeBid(productId, buyerId, nextBid);
                    if ("SUCCESS".equals(result)) {
                        LOG.info("[Thread-" + threadId + "] SUCCESS: Buyer " + buyerId + " placed ₹" + nextBid);
                        bidsPlaced++;
                    } else {
                        // Competition outbid us before we could commit
                        LOG.fine("[Thread-" + threadId + "] FAILED: Buyer " + buyerId + " - " + result);
                    }

                    // Sleep for a random interval between 0.5s to 2s
                    Thread.sleep(500 + random.nextInt(1500));
                } catch (InterruptedException e) {
                    LOG.info("[Thread-" + threadId + "] interrupted.");
                    break;
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "[Thread-" + threadId + "] Error", e);
                }
            }
            LOG.info("[Thread-" + threadId + "] finished.");
        }

        private double getCurrentBid(int pId) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT MAX(bid_amount) FROM bids WHERE product_id = ?")) {
                ps.setInt(1, pId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double val = rs.getDouble(1);
                        if (val > 0) return val;
                    }
                }
                // Fallback to base price
                try (PreparedStatement ps2 = conn.prepareStatement("SELECT base_price FROM products WHERE product_id = ?")) {
                    ps2.setInt(1, pId);
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) return rs2.getDouble(1);
                }
            } catch (Exception e) {
               // ignore
            }
            return 1000.0;
        }
    }
}
