package com.eauction.servlet;

import com.eauction.dao.UserDAO;
import com.eauction.model.User;
import com.eauction.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Already logged in → redirect to appropriate dashboard
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            redirectToDashboard((User) session.getAttribute("user"), req, resp);
            return;
        }
        req.getRequestDispatcher("/jsp/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String email    = ValidationUtil.param(req, "email").toLowerCase();
        String password = ValidationUtil.param(req, "password");

        if (email.isEmpty() || password.isEmpty()) {
            req.setAttribute("error", "Email and password are required.");
            req.getRequestDispatcher("/jsp/login.jsp").forward(req, resp);
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            req.setAttribute("error", "Please enter a valid e-mail address.");
            req.getRequestDispatcher("/jsp/login.jsp").forward(req, resp);
            return;
        }

        User user = userDAO.login(email, password);

        if (user == null) {
            req.setAttribute("error", "Invalid e-mail or password.");
            req.setAttribute("emailValue", ValidationUtil.escapeHtml(email));
            req.getRequestDispatcher("/jsp/login.jsp").forward(req, resp);
            return;
        }

        // Invalidate old session and create a fresh one (session fixation prevention)
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) oldSession.invalidate();

        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(3600);   // 60 minutes

        redirectToDashboard(user, req, resp);
    }

    private void redirectToDashboard(User user, HttpServletRequest req,
                                     HttpServletResponse resp) throws IOException {
        String ctx = req.getContextPath();
        switch (user.getRole()) {
            case "admin":  resp.sendRedirect(ctx + "/admin/dashboard");  break;
            case "seller": resp.sendRedirect(ctx + "/seller/dashboard"); break;
            default:       resp.sendRedirect(ctx + "/buyer/dashboard");
        }
    }
}
