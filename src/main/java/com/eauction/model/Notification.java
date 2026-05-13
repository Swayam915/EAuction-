package com.eauction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * In-app notification sent to a user (e.g. "You were outbid").
 */
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Notification type constants. */
    public static final String TYPE_OUTBID           = "OUTBID";
    public static final String TYPE_WON              = "WON";
    public static final String TYPE_AUCTION_ENDED    = "AUCTION_ENDED";
    public static final String TYPE_AUCTION_EXTENDED = "AUCTION_EXTENDED";

    private int           notifId;
    private int           userId;
    private Integer       productId;       // nullable
    private String        productName;     // joined, not stored
    private String        type;
    private String        message;
    private boolean       read;
    private LocalDateTime createdAt;

    public Notification() {}

    // ── Getters / Setters ──────────────────────────────────────────────────

    public int getNotifId()                           { return notifId; }
    public void setNotifId(int notifId)               { this.notifId = notifId; }

    public int getUserId()                            { return userId; }
    public void setUserId(int userId)                 { this.userId = userId; }

    public Integer getProductId()                     { return productId; }
    public void setProductId(Integer productId)       { this.productId = productId; }

    public String getProductName()                    { return productName; }
    public void setProductName(String productName)    { this.productName = productName; }

    public String getType()                           { return type; }
    public void setType(String type)                  { this.type = type; }

    public String getMessage()                        { return message; }
    public void setMessage(String message)            { this.message = message; }

    public boolean isRead()                           { return read; }
    public void setRead(boolean read)                 { this.read = read; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Relative display label based on type. */
    public String getIcon() {
        switch (type == null ? "" : type) {
            case TYPE_OUTBID:           return "📣";
            case TYPE_WON:              return "🏆";
            case TYPE_AUCTION_ENDED:    return "🔔";
            case TYPE_AUCTION_EXTENDED: return "⏱";
            default:                    return "ℹ️";
        }
    }
}
