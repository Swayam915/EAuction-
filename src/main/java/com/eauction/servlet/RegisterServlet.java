package com.eauction.servlet;

import com.eauction.dao.UserDAO;
import com.eauction.model.User;
import com.eauction.util.PasswordUtil;
import com.eauction.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Redirect already-logged-in users
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        req.getRequestDispatcher("/jsp/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String name            = ValidationUtil.param(req, "name");
        String email           = ValidationUtil.param(req, "email").toLowerCase();
        String password        = ValidationUtil.param(req, "password");
        String confirmPassword = ValidationUtil.param(req, "confirmPassword");
        String phone           = ValidationUtil.param(req, "phone");
        String address         = ValidationUtil.param(req, "address");
        String role            = ValidationUtil.param(req, "role");

        // ── Server-side validation ─────────────────────────────────────────
        String error = null;

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            error = "All required fields must be filled.";
        } else if (!ValidationUtil.isValidName(name)) {
            error = "Name must be 2–100 characters and contain only letters, spaces, hyphens, apostrophes, or dots.";
        } else if (!ValidationUtil.isValidEmail(email)) {
            error = "Please enter a valid e-mail address.";
        } else if (!PasswordUtil.meetsPolicy(password)) {
            error = "Password must be at least 8 characters and contain at least one letter and one digit.";
        } else if (!password.equals(confirmPassword)) {
            error = "Passwords do not match.";
        } else if (!phone.isEmpty() && !ValidationUtil.isValidPhone(phone)) {
            error = "Phone must be a valid 10-digit Indian mobile number starting with 6–9.";
        } else if (!"seller".equals(role) && !"buyer".equals(role)) {
            error = "Invalid role selected.";
        } else if (userDAO.emailExists(email)) {
            error = "An account with this e-mail already exists. Please login.";
        }

        if (error != null) {
            req.setAttribute("error", error);
            // Preserve safe form values so the user doesn't retype everything
            req.setAttribute("nameValue",    ValidationUtil.escapeHtml(name));
            req.setAttribute("emailValue",   ValidationUtil.escapeHtml(email));
            req.setAttribute("phoneValue",   ValidationUtil.escapeHtml(phone));
            req.setAttribute("addressValue", ValidationUtil.escapeHtml(address));
            req.setAttribute("roleValue",    ValidationUtil.escapeHtml(role));
            req.getRequestDispatcher("/jsp/register.jsp").forward(req, resp);
            return;
        }

        // ── Persist ────────────────────────────────────────────────────────
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);      // DAO will BCrypt-hash this
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setAddress(address.isEmpty() ? null : address);
        user.setRole(role);

        if (userDAO.register(user)) {
            req.setAttribute("success",
                "Registration successful! Please sign in with your new account.");
            req.getRequestDispatcher("/jsp/login.jsp").forward(req, resp);
        } else {
            req.setAttribute("error",
                "Registration failed due to a server error. Please try again.");
            req.getRequestDispatcher("/jsp/register.jsp").forward(req, resp);
        }
    }
}
