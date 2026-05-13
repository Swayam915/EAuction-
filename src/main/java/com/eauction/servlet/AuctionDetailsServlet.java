package com.eauction.servlet;

import com.eauction.dao.BidDAO;
import com.eauction.dao.ProductDAO;
import com.eauction.model.Product;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/auction/details")
public class AuctionDetailsServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final BidDAO     bidDAO     = new BidDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // AuthFilter already verifies authentication

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing auction id.");
            return;
        }

        try {
            int productId = Integer.parseInt(idStr.trim());
            Product product = productDAO.getProductById(productId);

            if (product == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Auction not found.");
                return;
            }

            req.setAttribute("product", product);
            req.setAttribute("bids",    bidDAO.getBidsByProduct(productId));
            req.getRequestDispatcher("/jsp/auction/details.jsp").forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid auction id.");
        }
    }
}
