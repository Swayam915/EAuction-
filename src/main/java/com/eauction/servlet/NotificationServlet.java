package com.eauction.servlet;

import com.eauction.model.Notification;
import com.eauction.model.User;
import com.eauction.service.NotificationService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GET  /notifications        → returns JSON list of recent notifications
 * POST /notifications?action=markRead&id=N  → marks one as read
 * POST /notifications?action=markAllRead    → marks all as read
 */
@WebServlet("/notifications")
public class NotificationServlet extends HttpServlet {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final NotificationService svc = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        User user = (User) req.getSession(false).getAttribute("user");
        List<Notification> list = svc.getRecent(user.getUserId());
        int unread              = svc.getUnreadCount(user.getUserId());

        PrintWriter out = resp.getWriter();
        out.print("{\"unread\":" + unread + ",\"notifications\":[");
        for (int i = 0; i < list.size(); i++) {
            Notification n = list.get(i);
            out.print("{"
                + "\"id\":"     + n.getNotifId()                                           + ","
                + "\"icon\":\"" + n.getIcon()                                              + "\","
                + "\"msg\":\""  + escape(n.getMessage())                                   + "\","
                + "\"time\":\"" + (n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "") + "\","
                + "\"read\":"   + n.isRead()
                + "}");
            if (i < list.size() - 1) out.print(",");
        }
        out.print("]}");
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        User user   = (User) req.getSession(false).getAttribute("user");
        String action = req.getParameter("action");

        if ("markAllRead".equals(action)) {
            svc.markAllRead(user.getUserId());
        } else if ("markRead".equals(action)) {
            String idStr = req.getParameter("id");
            if (idStr != null) {
                try { svc.markRead(Integer.parseInt(idStr), user.getUserId()); }
                catch (NumberFormatException ignored) {}
            }
        }

        resp.getWriter().print("{\"success\":true}");
        resp.getWriter().flush();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }
}
