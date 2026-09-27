package com.fixfirst;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notification_user_read", columnList = "userId,readAt"),
    uniqueConstraints = @UniqueConstraint(name = "uk_notification_event", columnNames = {"userId", "eventKey"}))
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false) public Long issueId;
    @Column(nullable = false, length = 80) public String eventKey;
    @Column(nullable = false, length = 300) public String message;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    public Instant readAt;
    protected Notification() {}
    public Notification(Long userId, Long issueId, String eventKey, String message) {
        this.userId = userId; this.issueId = issueId; this.eventKey = eventKey; this.message = message;
    }
}