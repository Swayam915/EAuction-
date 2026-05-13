package com.eauction.servlet;

import com.eauction.model.User;
import com.eauction.service.BidService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handles all bid submissions:
 *  POST /buyer/placeBid  → manual bid (increment-button or free-form)
 *  POST /buyer/autoBid   → save proxy/auto-bid max amount
 *
 * Tamper-prevention: the servlet NEVER trusts the "currentBid" value
 * sent from the browser. The live value is read from the DB inside
 * BidService, which also enforces the minimum increment server-side.
 */
@WebServlet(urlPatterns = {"/buyer/placeBid", "/buyer/autoBid"})
public class PlaceBidServlet extends HttpServlet {

    private final BidService bidService = new BidService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;

        if (user == null) {
            json(resp, false, "Please log in as a buyer to place bids.", null);
            return;
        }

        if ("/buyer/autoBid".equals(req.getServletPath())) {
            handleAutoBid(req, resp, user);
        } else {
            handleManualBid(req, resp, user);
        }
    }

    // ── Manual bid ─────────────────────────────────────────────────────────

    private void handleManualBid(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {

        String productIdStr = req.getParameter("productId");
        String bidAmountStr = req.getParameter("bidAmount");

        if (blank(productIdStr) || blank(bidAmountStr)) {
            json(resp, false, "Missing required parameters; productId and bidAmount are required.", null);
            return;
        }

        int productId; double bidAmount;
        try {
            productId = Integer.parseInt(productIdStr.trim());
            String normalized = bidAmountStr.trim().replaceAll("[^0-9.\\-]", "");
            bidAmount = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            json(resp, false, "Invalid bid format; please enter a numeric bid amount.", null);
            return;
        }

        if (bidAmount <= 0 || bidAmount > 1_00_00_00_000.0) {
            json(resp, false, "Bid amount is out of valid range.", null); return;
        }

        String result = bidService.placeBid(productId, user.getUserId(), bidAmount);
        if ("SUCCESS".equals(result)) {
            json(resp, true, String.format("Bid of \u20B9%,.2f placed!", bidAmount), bidAmount);
        } else {
            json(resp, false, result, null);
        }
    }

    // ── Auto bid ───────────────────────────────────────────────────────────

    private void handleAutoBid(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {

        String productIdStr = req.getParameter("productId");
        String maxAmountStr = req.getParameter("maxAmount");

        if (blank(productIdStr) || blank(maxAmountStr)) {
            json(resp, false, "Missing required parameters.", null); return;
        }

        int productId; double maxAmount;
        try {
            productId = Integer.parseInt(productIdStr.trim());
            maxAmount = Double.parseDouble(maxAmountStr.trim());
        } catch (NumberFormatException e) {
            json(resp, false, "Invalid amount format.", null); return;
        }

        if (maxAmount <= 0) {
            json(resp, false, "Max bid amount must be positive.", null); return;
        }

        String result = bidService.saveAutoBid(productId, user.getUserId(), maxAmount);
        if ("SUCCESS".equals(result)) {
            json(resp, true,
                String.format("Auto-bid set up to \u20B9%,.2f!", maxAmount), maxAmount);
        } else {
            json(resp, false, result, null);
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private void json(HttpServletResponse resp, boolean ok, String msg, Double bid)
            throws IOException {
        String esc = msg.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
        PrintWriter out = resp.getWriter();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":").append(ok)
          .append(",\"message\":\"").append(esc).append("\"");
        if (bid != null) sb.append(",\"newBid\":").append(String.format("%.2f", bid));
        sb.append("}");
        out.print(sb);
        out.flush();
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
