package com.fixfirst;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadAtIsNull(Long userId);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndEventKey(Long userId, String eventKey);
}