package com.fixfirst;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class ResolutionDeadlinePolicyTests {
    private final Instant created=Instant.parse("2026-09-27T10:00:00Z");
    private PriorityCalculator.Score score(String level) { return new PriorityCalculator.Score(0,0,0,0,level.equals("HIGH")?70:level.equals("MEDIUM")?40:10,level); }
    @Test void usesTheInitialEffectivePriorityOnly() {
        assertEquals(created.plus(Duration.ofHours(24)),ResolutionDeadlinePolicy.initialDeadline(score("HIGH"),created));
        assertEquals(created.plus(Duration.ofHours(72)),ResolutionDeadlinePolicy.initialDeadline(score("MEDIUM"),created));
        assertEquals(created.plus(Duration.ofDays(7)),ResolutionDeadlinePolicy.initialDeadline(score("LOW"),created));
    }
    @Test void oldIssuesMayHaveNoDeadline() { Issue issue=new Issue(); assertNull(issue.targetResolutionAt); }
}