package com.eauction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for obtaining and releasing JDBC connections.
 * All credentials are centralised here; change DB_URL, DB_USER, DB_PASS
 * to match your database installation.
 */
public final class DBConnection {

    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());

    private static final String DB_URL =
        "jdbc:h2:mem:eauction_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        + ";DATABASE_TO_LOWER=TRUE"
        + ";CASE_INSENSITIVE_IDENTIFIERS=TRUE";

    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";   // H2 default - no password

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                "H2 JDBC Driver not found on classpath: " + e.getMessage());
        }
    }

    private DBConnection() { /* utility class – no instantiation */ }

    /**
     * Returns a new Connection from DriverManager.
     * Callers are responsible for closing the connection (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ── Quiet-close helpers ────────────────────────────────────────────────

    public static void close(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) {
                LOG.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
    }

    public static void close(Statement st) {
        if (st != null) {
            try { st.close(); } catch (SQLException e) {
                LOG.log(Level.WARNING, "Failed to close Statement", e);
            }
        }
    }

    public static void close(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) {
                LOG.log(Level.WARNING, "Failed to close ResultSet", e);
            }
        }
    }

    /** Convenience: close RS + Statement + Connection in one call. */
    public static void close(ResultSet rs, Statement st, Connection conn) {
        close(rs);
        close(st);
        close(conn);
    }

    /** Roll back a connection quietly (used in catch blocks). */
    public static void rollback(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException e) {
                LOG.log(Level.WARNING, "Rollback failed", e);
            }
        }
    }
}
