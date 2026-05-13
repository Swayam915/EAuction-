package com.eauction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           resultId;
    private int           productId;
    private String        productName;
    private int           winnerId;
    private String        winnerName;
    private double        finalPrice;
    private LocalDateTime resultDate;

    public AuctionResult() {}

    // ── Getters / Setters ──────────────────────────────────────────────────

    public int getResultId()                          { return resultId; }
    public void setResultId(int resultId)             { this.resultId = resultId; }

    public int getProductId()                         { return productId; }
    public void setProductId(int productId)           { this.productId = productId; }

    public String getProductName()                    { return productName; }
    public void setProductName(String productName)    { this.productName = productName; }

    public int getWinnerId()                          { return winnerId; }
    public void setWinnerId(int winnerId)             { this.winnerId = winnerId; }

    public String getWinnerName()                     { return winnerName; }
    public void setWinnerName(String winnerName)      { this.winnerName = winnerName; }

    public double getFinalPrice()                     { return finalPrice; }
    public void setFinalPrice(double finalPrice)      { this.finalPrice = finalPrice; }

    public LocalDateTime getResultDate()              { return resultDate; }
    public void setResultDate(LocalDateTime resultDate){ this.resultDate = resultDate; }
}
