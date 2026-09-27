package com.fixfirst;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface IssueRepository extends JpaRepository<Issue, Long> {
    // Lock one issue during a change so simultaneous votes/status changes stay consistent.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Issue i where i.id = :id")
    Optional<Issue> findLocked(@Param("id") Long id);
}
