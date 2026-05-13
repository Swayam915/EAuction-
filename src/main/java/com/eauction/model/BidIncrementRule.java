package com.eauction.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries the computed minimum-increment and the list of preset
 * increment buttons to show the buyer.
 *
 * Rules (Feature 2 — Dynamic Bid Increment Logic):
 *   currentBid <   1,000  → minimum increment =    50
 *   currentBid <  10,000  → minimum increment =   500
 *   currentBid < 100,000  → minimum increment = 1,000
 *   currentBid >= 100,000 → minimum increment = 5,000
 */
public class BidIncrementRule {

    private final double minIncrement;
    private final List<double[]> buttons;   // each entry: [label-amount, total-bid]

    private BidIncrementRule(double minIncrement, List<double[]> buttons) {
        this.minIncrement = minIncrement;
        this.buttons      = buttons;
    }

    // ── Factory ────────────────────────────────────────────────────────────

    public static BidIncrementRule forCurrentBid(double currentBid) {
        double min;
        double[] steps;

        if (currentBid < 1_000) {
            min   = 50;
            steps = new double[]{50, 100, 250, 500};
        } else if (currentBid < 10_000) {
            min   = 500;
            steps = new double[]{500, 1_000, 2_000, 5_000};
        } else if (currentBid < 1_00_000) {
            min   = 1_000;
            steps = new double[]{1_000, 2_000, 5_000, 10_000};
        } else {
            min   = 5_000;
            steps = new double[]{5_000, 10_000, 25_000, 50_000};
        }

        List<double[]> buttons = new ArrayList<>();
        for (double step : steps) {
            buttons.add(new double[]{step, currentBid + step});
        }
        return new BidIncrementRule(min, buttons);
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public double getMinIncrement()         { return minIncrement; }
    public double getMinValidBid(double currentBid) { return currentBid + minIncrement; }
    public List<double[]> getButtons()      { return buttons; }
}
