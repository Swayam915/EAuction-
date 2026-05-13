package com.eauction.service;

import com.eauction.dao.NotificationDAO;
import com.eauction.model.Notification;

import java.util.List;

/**
 * Service facade for notification operations.
 * Keeps servlets clean — they call service, not DAO directly.
 */
public class NotificationService {

    private final NotificationDAO dao = new NotificationDAO();

    public List<Notification> getRecent(int userId) {
        return dao.getRecentNotifications(userId, 20);
    }

    public int getUnreadCount(int userId) {
        return dao.getUnreadCount(userId);
    }

    public void markAllRead(int userId) {
        dao.markAllRead(userId);
    }

    public void markRead(int notifId, int userId) {
        dao.markRead(notifId, userId);
    }
}
