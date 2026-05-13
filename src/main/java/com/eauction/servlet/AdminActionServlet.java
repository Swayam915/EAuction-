package com.eauction.servlet;

import com.eauction.dao.ProductDAO;
import com.eauction.dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/action")
public class AdminActionServlet extends HttpServlet {

    private final UserDAO    userDAO    = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private static final com.eauction.service.BidSimulator simulator = new com.eauction.service.BidSimulator();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        // AuthFilter already enforces admin role

        String action = req.getParameter("action");
        String idStr  = req.getParameter("id");

        if (action == null || idStr == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            return;
        }

        try {
            int id = Integer.parseInt(idStr.trim());
            switch (action) {
                case "toggleUser":
                    userDAO.toggleUserStatus(id);
                    break;
                case "closeAuction":
                    productDAO.updateProductStatus(id, "closed");
                    break;
                case "activateAuction":
                    productDAO.updateProductStatus(id, "active");
                    break;
                case "cancelAuction":
                    productDAO.updateProductStatus(id, "cancelled");
                    break;
                case "startSimulation":
                    simulator.startSimulation(id, 5, 20); // 5 threads, 20 max bids per thread
                    try { Thread.sleep(2000); } catch(Exception e){} // slight delay for visual impact
                    break;
                case "stopSimulation":
                    simulator.stopSimulation();
                    break;
                default:
                    // Unknown action – silently ignore
            }
        } catch (NumberFormatException e) {
            // Bad id – ignore
        }

        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }
}
