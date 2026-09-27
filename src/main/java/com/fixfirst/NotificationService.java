package com.fixfirst;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications; this.users = users;
    }
    public void notifyOnce(Long userId, Long issueId, String key, String message) {
        if (userId == null) return;
        if (notifications.existsByUserIdAndEventKey(userId, key)) return;
        try { notifications.save(new Notification(userId, issueId, key, message)); }
        catch (DataIntegrityViolationException ignored) { }
    }
    public void notifyAdmins(Long issueId, String key, String message) {
        users.findByRole("ADMIN").forEach(u -> notifyOnce(u.id, issueId, key + ":" + u.id, message));
    }
    public List<Notification> list(Long userId) { return notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId); }
    public long unread(Long userId) { return notifications.countByUserIdAndReadAtIsNull(userId); }
    public void markRead(Long id, Long userId) {
        Notification n=notifications.findByIdAndUserId(id,userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found.")); n.readAt=java.time.Instant.now();
    }
    public void markAllRead(Long userId) { list(userId).stream().filter(n -> n.readAt == null).forEach(n -> n.readAt=java.time.Instant.now()); }
}