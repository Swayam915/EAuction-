package com.eauction.servlet;

import com.eauction.dao.AuctionResultDAO;
import com.eauction.dao.ProductDAO;
import com.eauction.dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private final UserDAO          userDAO   = new UserDAO();
    private final ProductDAO       productDAO = new ProductDAO();
    private final AuctionResultDAO resultDAO  = new AuctionResultDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // AuthFilter already enforces admin role

        req.setAttribute("totalUsers",    userDAO.getTotalUsers());
        req.setAttribute("activeAuctions",productDAO.getActiveAuctionCount());
        req.setAttribute("totalRevenue",  productDAO.getTotalRevenue());
        req.setAttribute("allUsers",      userDAO.getAllUsers());
        req.setAttribute("allProducts",   productDAO.getAllProducts());
        req.setAttribute("allResults",    resultDAO.getAllResults());

        req.getRequestDispatcher("/jsp/admin/dashboard.jsp").forward(req, resp);
    }
}
