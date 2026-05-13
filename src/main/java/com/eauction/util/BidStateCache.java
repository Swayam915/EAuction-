package com.eauction.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Lightweight in-memory store for current product bid prices.
 * (Non-distributed; useful for single JVM topology in this sample app.)
 */
public final class BidStateCache {

    private static final ConcurrentMap<Integer, Double> LATEST_BIDS = new ConcurrentHashMap<>();

    private BidStateCache() { }

    public static double getCurrentBid(int productId) {
        Double current = LATEST_BIDS.get(productId);
        return current == null ? 0.0 : current;
    }

    public static void setCurrentBid(int productId, double bidAmount) {
        if (productId <= 0 || bidAmount <= 0) return;
        LATEST_BIDS.put(productId, bidAmount);
    }

    public static void clear(int productId) {
        if (productId > 0) LATEST_BIDS.remove(productId);
    }

    public static void clearAll() {
        LATEST_BIDS.clear();
    }
}
