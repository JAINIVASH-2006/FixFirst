package com.fixfirst;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class PriorityCalculatorTests {
    private final Instant now=Instant.parse("2026-09-26T12:00:00Z");
    private Issue issue(int days) {
        Issue i=new Issue();i.category=Issue.Category.WATER;i.createdAt=now.minus(Duration.ofDays(days));return i;
    }
    @Test void scoreCapsPreventPopularityOrAgeFromGrowingWithoutBound() {
        Issue i=issue(200);for(long n=1;n<=100;n++)i.supporters.add(n);
        assertEquals(100,PriorityCalculator.calculate(i,now).effective());
    }
    @Test void partialDaysDoNotCountAndFutureDatesDoNotSubtractPoints() {
        Issue i=issue(0);i.createdAt=now.minus(Duration.ofHours(23));
        assertEquals(0,PriorityCalculator.calculate(i,now).agePoints());
        i.createdAt=now.plusSeconds(60);assertEquals(0,PriorityCalculator.calculate(i,now).agePoints());
    }
    @Test void resolvedIssueStopsAgeingAndOverrideKeepsExplanation() {
        Issue i=issue(5);i.resolvedAt=i.createdAt.plus(Duration.ofDays(2));i.overrideScore=95;
        var s=PriorityCalculator.calculate(i,now);assertEquals(6,s.agePoints());assertEquals(46,s.calculated());assertEquals(95,s.effective());
    }
    @Test void reopeningRestoresAgeAndLevelBoundariesAreExplicit() {
        Issue i=issue(10);i.resolvedAt=null;
        assertEquals("HIGH",PriorityCalculator.calculate(i,now).level());
        i.createdAt=now;assertEquals("MEDIUM",PriorityCalculator.calculate(i,now).level());
        i.category=Issue.Category.OTHER;assertEquals("LOW",PriorityCalculator.calculate(i,now).level());
    }
}
