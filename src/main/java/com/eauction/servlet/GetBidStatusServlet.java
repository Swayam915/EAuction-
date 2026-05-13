package com.eauction.servlet;

import com.eauction.dao.BidDAO;
import com.eauction.dao.ProductDAO;
import com.eauction.model.BidIncrementRule;
import com.eauction.model.Product;
import com.eauction.util.BidStateCache;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * AJAX polling endpoint returning current bid + increment buttons.
 * GET /auction/status?productId=N
 */
@WebServlet("/auction/status")
public class GetBidStatusServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final BidDAO     bidDAO     = new BidDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control","no-store");

        PrintWriter out   = resp.getWriter();
        String      idStr = req.getParameter("productId");

        if (idStr == null || idStr.isBlank()) {
            out.print("{\"error\":\"Missing productId\"}"); return;
        }

        try {
            int     pid     = Integer.parseInt(idStr.trim());
            Product product = productDAO.getProductById(pid);
            if (product == null) { out.print("{\"error\":\"Product not found\"}"); return; }

            double           high   = bidDAO.getHighestBid(pid);
            double           cached = BidStateCache.getCurrentBid(pid);
            double           cur    = high > 0 ? high : product.getBasePrice();
            if (cached > cur) cur = cached;
            BidIncrementRule rule   = BidIncrementRule.forCurrentBid(cur);
            String           endT   = product.getAuctionEndTime() != null
                                      ? product.getAuctionEndTime().toString() : "";

            // Build buttons JSON
            List<double[]> btns = rule.getButtons();
            StringBuilder btnsJson = new StringBuilder("[");
            for (int i = 0; i < btns.size(); i++) {
                double[] b = btns.get(i);
                btnsJson.append("{\"label\":\"+\u20B9").append(fmt(b[0]))
                        .append("\",\"amount\":").append(String.format("%.2f", b[1]))
                        .append(",\"step\":").append(String.format("%.2f", b[0])).append("}");
                if (i < btns.size()-1) btnsJson.append(",");
            }
            btnsJson.append("]");

            out.printf("{\"productId\":%d,\"currentBid\":%.2f,\"status\":\"%s\","
                     + "\"endTime\":\"%s\",\"minIncrement\":%.2f,\"buttons\":%s}",
                pid, cur, product.getStatus(), endT, rule.getMinIncrement(), btnsJson);

        } catch (NumberFormatException e) {
            out.print("{\"error\":\"Invalid productId\"}");
        }
        out.flush();
    }

    private String fmt(double v) {
        return String.format("%,.0f", v);
    }
}
