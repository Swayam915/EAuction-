package com.eauction.util;

import com.eauction.dao.ProductDAO;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton background timer that scans for expired auctions every 60 seconds
 * and closes them automatically (declares winner, records auction_result row).
 *
 * Lifecycle is managed by {@link com.eauction.servlet.AppStartup}.
 */
public final class AuctionTimer {

    private static final Logger LOG = Logger.getLogger(AuctionTimer.class.getName());

    private static volatile AuctionTimer instance;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auction-timer");
            t.setDaemon(true);          // does not block JVM shutdown
            return t;
        });

    @SuppressWarnings("unused")
    private ScheduledFuture<?> future;

    private AuctionTimer() {}

    public static AuctionTimer getInstance() {
        if (instance == null) {
            synchronized (AuctionTimer.class) {
                if (instance == null) instance = new AuctionTimer();
            }
        }
        return instance;
    }

    public void start() {
        future = scheduler.scheduleAtFixedRate(() -> {
            try {
                new ProductDAO().processExpiredAuctions();
                LOG.fine("AuctionTimer: expired-auction check complete.");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "AuctionTimer: error while processing expired auctions", e);
            }
        }, 10, 60, TimeUnit.SECONDS);

        LOG.info("AuctionTimer started – checking every 60 seconds.");
    }

    public void stop() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("AuctionTimer stopped.");
    }
}
