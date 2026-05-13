package com.eauction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           productId;
    private int           sellerId;
    private String        sellerName;
    private String        productName;
    private String        description;
    private double        basePrice;
    private double        currentBid;
    private String        category;
    private String        image;
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;
    private String        status;          // pending | active | closed | cancelled
    private LocalDateTime createdAt;

    public Product() {}

    // ── Getters / Setters ──────────────────────────────────────────────────

    public int getProductId()                         { return productId; }
    public void setProductId(int productId)           { this.productId = productId; }

    public int getSellerId()                          { return sellerId; }
    public void setSellerId(int sellerId)             { this.sellerId = sellerId; }

    public String getSellerName()                     { return sellerName; }
    public void setSellerName(String sellerName)      { this.sellerName = sellerName; }

    public String getProductName()                    { return productName; }
    public void setProductName(String productName)    { this.productName = productName; }

    public String getDescription()                    { return description; }
    public void setDescription(String description)    { this.description = description; }

    public double getBasePrice()                      { return basePrice; }
    public void setBasePrice(double basePrice)        { this.basePrice = basePrice; }

    public double getCurrentBid()                     { return currentBid; }
    public void setCurrentBid(double currentBid)      { this.currentBid = currentBid; }

    public String getCategory()                       { return category; }
    public void setCategory(String category)          { this.category = category; }

    public String getImage()                          { return image; }
    public void setImage(String image)                { this.image = image; }

    public LocalDateTime getAuctionStartTime()        { return auctionStartTime; }
    public void setAuctionStartTime(LocalDateTime t)  { this.auctionStartTime = t; }

    public LocalDateTime getAuctionEndTime()          { return auctionEndTime; }
    public void setAuctionEndTime(LocalDateTime t)    { this.auctionEndTime = t; }

    public String getStatus()                         { return status; }
    public void setStatus(String status)              { this.status = status; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── Convenience ───────────────────────────────────────────────────────

    /**
     * True only when the auction is marked active AND the current
     * server time is within the scheduled window.
     */
    public boolean isLive() {
        if (!"active".equals(status)) return false;
        LocalDateTime now = LocalDateTime.now();
        return auctionStartTime != null && auctionEndTime != null
            && !now.isBefore(auctionStartTime)
            && now.isBefore(auctionEndTime);
    }

    /** Effective price to display: max(currentBid, basePrice). */
    public double getEffectivePrice() {
        return Math.max(currentBid, basePrice);
    }
}
