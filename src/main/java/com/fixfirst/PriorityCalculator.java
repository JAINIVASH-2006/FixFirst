package com.fixfirst;

import java.time.*;

/** Transparent teaching rules, not a validated emergency-triage model. */
public final class PriorityCalculator {
    private PriorityCalculator() {}
    public record Score(int categoryPoints, int supportPoints, int agePoints,
                        int calculated, int effective, String level) {}
    public static Score calculate(Issue issue, Instant now) {
        int category = switch (issue.category) {
            case WATER, ELECTRICAL -> 40;
            case SANITATION, ACCESSIBILITY -> 30;
            case EQUIPMENT -> 20;
            case OTHER -> 10;
        };
        int support = Math.min(issue.supporters.size(), 15) * 2;
        // Freeze ageing while resolved/closed. Reopening restores total age since reporting.
        Instant end = issue.resolvedAt == null ? now : issue.resolvedAt;
        long days = Math.max(0, Duration.between(issue.createdAt, end).toDays());
        int age = (int) Math.min(days, 10) * 3;
        int calculated = category + support + age;
        int effective = issue.overrideScore == null ? calculated : issue.overrideScore;
        String level = effective >= 70 ? "HIGH" : effective >= 40 ? "MEDIUM" : "LOW";
        return new Score(category, support, age, calculated, effective, level);
    }
}
