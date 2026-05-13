package com.eauction.filter;

import com.eauction.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Servlet Filter that enforces role-based access control.
 *
 * URL rules
 * ─────────
 *  /admin/*   → only "admin"
 *  /seller/*  → only "seller"
 *  /buyer/*   → only "buyer"
 *  /auction/* → any authenticated user
 *  /profile/* → any authenticated user
 *
 * Unauthenticated requests are redirected to /login.
 * Role-mismatched requests are redirected to the user's own dashboard.
 */
@WebFilter(urlPatterns = {"/admin/*", "/seller/*", "/buyer/*", "/auction/*", "/profile/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Prevent caching of protected pages
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma",        "no-cache");
        resp.setDateHeader("Expires",   0);

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String path = req.getServletPath();   // e.g. "/admin/dashboard"
        String ctx  = req.getContextPath();

        boolean allowed = false;

        if (path.startsWith("/admin/")) {
            allowed = user.isAdmin();
        } else if (path.startsWith("/seller/")) {
            allowed = user.isSeller();
        } else if (path.startsWith("/buyer/")) {
            allowed = user.isBuyer();
        } else {
            // /auction/*, /profile/* — any authenticated role
            allowed = true;
        }

        if (!allowed) {
            // Redirect to the user's own dashboard instead of showing 403
            resp.sendRedirect(ctx + dashboardFor(user));
            return;
        }

        chain.doFilter(request, response);
    }

    private String dashboardFor(User u) {
        if (u.isAdmin())  return "/admin/dashboard";
        if (u.isSeller()) return "/seller/dashboard";
        return "/buyer/dashboard";
    }

    @Override public void init(FilterConfig cfg) {}
    @Override public void destroy() {}
}
