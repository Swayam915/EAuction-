package com.eauction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a proxy (auto) bid set by a buyer for a product.
 * The system will automatically raise the buyer's bid up to maxAmount
 * whenever a competing bid arrives.
 */
public class AutoBid implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           autoBidId;
    private int           productId;
    private int           buyerId;
    private String        buyerName;
    private double        maxAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean       active;

    public AutoBid() {}

    // ── Getters / Setters ──────────────────────────────────────────────────

    public int getAutoBidId()                          { return autoBidId; }
    public void setAutoBidId(int autoBidId)            { this.autoBidId = autoBidId; }

    public int getProductId()                          { return productId; }
    public void setProductId(int productId)            { this.productId = productId; }

    public int getBuyerId()                            { return buyerId; }
    public void setBuyerId(int buyerId)                { this.buyerId = buyerId; }

    public String getBuyerName()                       { return buyerName; }
    public void setBuyerName(String buyerName)         { this.buyerName = buyerName; }

    public double getMaxAmount()                       { return maxAmount; }
    public void setMaxAmount(double maxAmount)         { this.maxAmount = maxAmount; }

    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)  { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)  { this.updatedAt = updatedAt; }

    public boolean isActive()                          { return active; }
    public void setActive(boolean active)              { this.active = active; }
}
