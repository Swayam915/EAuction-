package com.eauction.servlet;

import com.eauction.dao.AuctionResultDAO;
import com.eauction.dao.BidDAO;
import com.eauction.dao.ProductDAO;
import com.eauction.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/buyer/dashboard")
public class BuyerDashboardServlet extends HttpServlet {

    private final ProductDAO       productDAO = new ProductDAO();
    private final BidDAO           bidDAO     = new BidDAO();
    private final AuctionResultDAO resultDAO  = new AuctionResultDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // AuthFilter already enforces buyer role
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");

        req.setAttribute("activeProducts", productDAO.getAllActiveProducts());
        req.setAttribute("myBids",         bidDAO.getBidsByBuyer(user.getUserId()));
        req.setAttribute("wonAuctions",    resultDAO.getResultsByWinner(user.getUserId()));

        req.getRequestDispatcher("/jsp/buyer/dashboard.jsp").forward(req, resp);
    }
}
