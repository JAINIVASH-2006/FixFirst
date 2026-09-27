package com.fixfirst;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "issue_photos")
public class IssuePhoto {
    public enum Kind { BEFORE, AFTER }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long issueId;
    @Column(nullable = false) public Long uploaderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Kind kind;
    @Column(nullable = false, length = 120) public String storedName;
    @Column(nullable = false, length = 80) public String contentType;
    @Column(nullable = false) public Instant uploadedAt = Instant.now();
    protected IssuePhoto() {}
    public IssuePhoto(Long issueId, Long uploaderId, Kind kind, String storedName, String contentType) {
        this.issueId = issueId; this.uploaderId = uploaderId; this.kind = kind;
        this.storedName = storedName; this.contentType = contentType;
    }
}