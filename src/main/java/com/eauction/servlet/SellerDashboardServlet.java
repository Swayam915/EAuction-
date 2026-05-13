package com.eauction.servlet;

import com.eauction.dao.ProductDAO;
import com.eauction.model.Product;
import com.eauction.model.User;
import com.eauction.service.ImageUploadService;
import com.eauction.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@WebServlet("/seller/dashboard")
@MultipartConfig(maxFileSize=5*1024*1024, maxRequestSize=6*1024*1024, fileSizeThreshold=1024*1024)
public class SellerDashboardServlet extends HttpServlet {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");

        req.setAttribute("products", productDAO.getProductsBySeller(user.getUserId()));
        req.getRequestDispatcher("/jsp/seller/dashboard.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        if ("addProduct".equals(ValidationUtil.param(req, "action"))) {
            handleAddProduct(req, resp, user);
        } else {
            resp.sendRedirect(req.getContextPath() + "/seller/dashboard");
        }
    }

    private void handleAddProduct(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {

        String productName  = ValidationUtil.param(req, "productName");
        String description  = ValidationUtil.param(req, "description");
        String basePriceStr = ValidationUtil.param(req, "basePrice");
        String category     = ValidationUtil.param(req, "category");
        String startStr     = ValidationUtil.param(req, "auctionStartTime");
        String endStr       = ValidationUtil.param(req, "auctionEndTime");

        String error = null;
        if (productName.isEmpty()||basePriceStr.isEmpty()||startStr.isEmpty()||endStr.isEmpty())
            error = "Product name, base price, and auction times are required.";

        double basePrice = 0; LocalDateTime startTime = null, endTime = null;
        if (error == null) {
            try { basePrice = Double.parseDouble(basePriceStr);
                  if (basePrice <= 0) error = "Base price must be > 0.";
            } catch (NumberFormatException e) { error = "Invalid base price."; }
        }
        if (error == null) {
            try { startTime = LocalDateTime.parse(startStr, DT_FMT);
                  endTime   = LocalDateTime.parse(endStr, DT_FMT);
            } catch (DateTimeParseException e) { error = "Invalid date/time format."; }
        }
        if (error == null) {
            if (!endTime.isAfter(startTime)) error = "End time must be after start time.";
            else if (endTime.isBefore(LocalDateTime.now())) error = "End time must be in the future.";
        }

        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute("products", productDAO.getProductsBySeller(user.getUserId()));
            req.getRequestDispatcher("/jsp/seller/dashboard.jsp").forward(req, resp); return;
        }

        // Image upload
        String imagePath = null;
        try {
            Part imagePart = req.getPart("productImage");
            if (imagePart != null && imagePart.getSize() > 0) {
                ImageUploadService up = new ImageUploadService(getServletContext().getRealPath("/"));
                imagePath = up.saveImage(imagePart);
                if (imagePath == null) {
                    req.setAttribute("error","Image must be JPG/PNG/WebP under 5 MB.");
                    req.setAttribute("products", productDAO.getProductsBySeller(user.getUserId()));
                    req.getRequestDispatcher("/jsp/seller/dashboard.jsp").forward(req, resp); return;
                }
            }
        } catch (Exception ignored) {}

        Product p = new Product();
        p.setSellerId(user.getUserId());
        p.setProductName(productName);
        p.setDescription(description.isEmpty() ? null : description);
        p.setBasePrice(basePrice);
        p.setCategory(category.isEmpty() ? "Other" : category);
        p.setImage(imagePath);
        p.setAuctionStartTime(startTime);
        p.setAuctionEndTime(endTime);
        p.setStatus(startTime.isBefore(LocalDateTime.now()) ? "active" : "pending");

        if (productDAO.addProduct(p)) {
            req.setAttribute("success","\"" + ValidationUtil.escapeHtml(productName) + "\" listed!");
        } else {
            req.setAttribute("error","Failed to list product. Try again.");
        }
        req.setAttribute("products", productDAO.getProductsBySeller(user.getUserId()));
        req.getRequestDispatcher("/jsp/seller/dashboard.jsp").forward(req, resp);
    }
}
