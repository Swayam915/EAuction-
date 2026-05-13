package com.eauction.servlet;

import com.eauction.util.AuctionTimer;
import com.eauction.util.DatabaseInitializer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * Starts / stops background services when the web application is
 * deployed or undeployed.
 */
@WebListener
public class AppStartup implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartup.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Initialize database first
        DatabaseInitializer.initializeDatabase();

        // Start background services
        AuctionTimer.getInstance().start();
        LOG.info("=== E-Auction Application Started. Database initialized and AuctionTimer started. ===");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        AuctionTimer.getInstance().stop();
        LOG.info("=== E-Auction Application Stopped. ===");
    }
}
