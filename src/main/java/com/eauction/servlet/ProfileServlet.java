package com.eauction.servlet;

import com.eauction.dao.UserDAO;
import com.eauction.model.User;
import com.eauction.util.PasswordUtil;
import com.eauction.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Handles:
 *   GET  /profile        → show profile page
 *   POST /profile?action=updateProfile   → save name/phone/address
 *   POST /profile?action=changePassword  → change password
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // AuthFilter already verified the user is logged in
        req.getRequestDispatcher("/jsp/common/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        User currentUser    = (User) session.getAttribute("user");

        String action = ValidationUtil.param(req, "action");

        switch (action) {
            case "updateProfile":
                handleUpdateProfile(req, resp, session, currentUser);
                break;
            case "changePassword":
                handleChangePassword(req, resp, currentUser);
                break;
            default:
                resp.sendRedirect(req.getContextPath() + "/profile");
        }
    }

    // ── Update name / phone / address ──────────────────────────────────────

    private void handleUpdateProfile(HttpServletRequest req, HttpServletResponse resp,
                                     HttpSession session, User currentUser)
            throws ServletException, IOException {

        String name    = ValidationUtil.param(req, "name");
        String phone   = ValidationUtil.param(req, "phone");
        String address = ValidationUtil.param(req, "address");

        if (!ValidationUtil.isValidName(name)) {
            req.setAttribute("profileError",
                "Name must be 2–100 characters (letters, spaces, hyphens, apostrophes).");
            req.getRequestDispatcher("/jsp/common/profile.jsp").forward(req, resp);
            return;
        }

        if (!phone.isEmpty() && !ValidationUtil.isValidPhone(phone)) {
            req.setAttribute("profileError",
                "Phone must be a valid 10-digit Indian mobile number.");
            req.getRequestDispatcher("/jsp/common/profile.jsp").forward(req, resp);
            return;
        }

        User updated = userDAO.updateProfile(currentUser.getUserId(), name, phone, address);
        if (updated != null) {
            // Refresh the session object so the top-bar shows the new name immediately
            session.setAttribute("user", updated);
            req.setAttribute("profileSuccess", "Profile updated successfully.");
        } else {
            req.setAttribute("profileError", "Failed to update profile. Please try again.");
        }
        req.getRequestDispatcher("/jsp/common/profile.jsp").forward(req, resp);
    }

    // ── Change password ────────────────────────────────────────────────────

    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp,
                                      User currentUser)
            throws ServletException, IOException {

        String current    = ValidationUtil.param(req, "currentPassword");
        String newPass    = ValidationUtil.param(req, "newPassword");
        String confirmNew = ValidationUtil.param(req, "confirmNewPassword");

        if (current.isEmpty() || newPass.isEmpty() || confirmNew.isEmpty()) {
            req.setAttribute("passwordError", "All password fields are required.");
            req.getRequestDispatcher("/jsp/common/profile.jsp").forward(req, resp);
            return;
        }

        if (!PasswordUtil.meetsPolicy(newPass)) {
            req.setAttribute("passwordError",
                "New password must be at least 8 characters with at least one letter and one digit.");
            req.getRequestDispatcher("/jsp/common/profile.jsp").forward(req, resp);
            return;
        }

        if (!newPass.equals(confirmNew)) {
            req.setAttribute("passwordError", "New passwords do not match.");
            req.getRequestDispatcher("/jsp/common/profile.jsp").forward(req, resp);
            return;
        }

        String result = userDAO.changePassword(currentUser.getUserId(), current, newPass);
        switch (result) {
            case "SUCCESS":
                req.setAttribute("passwordSuccess",
                    "Password changed successfully. Please use the new password on next login.");
                break;
            case "WRONG_CURRENT":
                req.setAttribute("passwordError", "Current password is incorrect.");
                break;
            default:
                req.setAttribute("passwordError",
                    "Failed to change password. Please try again.");
        }
        req.getRequestDispatcher("/jsp/common/profile.jsp").forward(req, resp);
    }
}
