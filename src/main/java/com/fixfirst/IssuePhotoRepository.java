package com.fixfirst;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuePhotoRepository extends JpaRepository<IssuePhoto, Long> {
    List<IssuePhoto> findByIssueIdOrderByUploadedAtAsc(Long issueId);
    long countByIssueIdAndKind(Long issueId, IssuePhoto.Kind kind);
    Optional<IssuePhoto> findByStoredName(String storedName);
}