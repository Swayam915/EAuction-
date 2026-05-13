package com.eauction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Bid implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           bidId;
    private int           productId;
    private String        productName;
    private int           buyerId;
    private String        buyerName;
    private double        bidAmount;
    private LocalDateTime bidTime;

    public Bid() {}

    public Bid(int productId, int buyerId, double bidAmount) {
        this.productId = productId;
        this.buyerId   = buyerId;
        this.bidAmount = bidAmount;
        this.bidTime   = LocalDateTime.now();
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public int getBidId()                        { return bidId; }
    public void setBidId(int bidId)              { this.bidId = bidId; }

    public int getProductId()                    { return productId; }
    public void setProductId(int productId)      { this.productId = productId; }

    public String getProductName()               { return productName; }
    public void setProductName(String n)         { this.productName = n; }

    public int getBuyerId()                      { return buyerId; }
    public void setBuyerId(int buyerId)          { this.buyerId = buyerId; }

    public String getBuyerName()                 { return buyerName; }
    public void setBuyerName(String buyerName)   { this.buyerName = buyerName; }

    public double getBidAmount()                 { return bidAmount; }
    public void setBidAmount(double bidAmount)   { this.bidAmount = bidAmount; }

    public LocalDateTime getBidTime()            { return bidTime; }
    public void setBidTime(LocalDateTime t)      { this.bidTime = t; }
}
