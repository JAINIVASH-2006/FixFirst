package com.fixfirst;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "issues")
public class Issue {
    public enum Category { WATER, ELECTRICAL, SANITATION, ACCESSIBILITY, EQUIPMENT, OTHER }
    public enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 120) public String title;
    @Column(nullable = false, length = 1500) public String description;
    @Column(nullable = false, length = 80) public String location;
    @Column(nullable = false, length = 80) public String equipment;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Category category;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Status status = Status.OPEN;
    @Column(nullable = false) public Long reporterId;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    public Instant resolvedAt;
    public Instant firstResolvedAt;
    public Long assignedTechnicianId;
    public Instant targetResolutionAt;
    public Long assetId;
    @Column(precision = 12, scale = 2) public java.math.BigDecimal repairCost;
    public Integer reopenCount = 0;
    public Integer overrideScore;
    @Column(length = 300) public String overrideReason;
    @ElementCollection
    @CollectionTable(name = "issue_supports", joinColumns = @JoinColumn(name = "issue_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"issue_id", "user_id"}))
    @Column(name = "user_id", nullable = false)
    public Set<Long> supporters = new HashSet<>();
    @ElementCollection
    @CollectionTable(name = "issue_history", joinColumns = @JoinColumn(name = "issue_id"))
    @OrderColumn(name = "event_order") @Column(name = "event_text", length = 600)
    public List<String> history = new ArrayList<>();
    public Issue() {}
}
