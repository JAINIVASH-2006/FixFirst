package com.fixfirst;

import java.time.*;

public final class ResolutionDeadlinePolicy {
    private ResolutionDeadlinePolicy() {}
    public static Instant initialDeadline(PriorityCalculator.Score score, Instant createdAt) {
        return createdAt.plus(switch (score.level()) {
            case "HIGH" -> Duration.ofHours(24);
            case "MEDIUM" -> Duration.ofHours(72);
            default -> Duration.ofDays(7);
        });
    }
}