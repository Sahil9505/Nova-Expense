package com.nova.finance.goal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GoalCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 17);
    private static final LocalDate FUTURE = LocalDate.of(2026, 12, 31);
    private static final LocalDate PAST = LocalDate.of(2026, 1, 1);

    @Test
    void percentageAndRemainingScaleCorrectly() {
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1000"), new BigDecimal("250"), Goal.Type.SAVINGS,
                false, FUTURE, null, null, TODAY);
        assertEquals(new BigDecimal("25.00"), calc.percentageComplete());
        assertEquals(new BigDecimal("750.00"), calc.remaining());
    }

    @Test
    void notStartedWhenNothingSaved() {
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1000"), BigDecimal.ZERO, Goal.Type.SAVINGS,
                false, FUTURE, null, null, TODAY);
        assertEquals(GoalStatus.NOT_STARTED, calc.status());
        assertEquals(BigDecimal.ZERO, calc.percentageComplete());
    }

    @Test
    void inProgressBetweenZeroAndTarget() {
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1000"), new BigDecimal("1"), Goal.Type.SAVINGS,
                false, FUTURE, null, null, TODAY);
        assertEquals(GoalStatus.IN_PROGRESS, calc.status());
    }

    @Test
    void achievedWhenCurrentMeetsTarget() {
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1000"), new BigDecimal("1000"), Goal.Type.SAVINGS,
                false, FUTURE, null, null, TODAY);
        assertEquals(GoalStatus.ACHIEVED, calc.status());
        assertEquals(new BigDecimal("100.00"), calc.percentageComplete());
        assertEquals(BigDecimal.ZERO, calc.remaining());
    }

    @Test
    void achievedTakesPrecedenceOverPausedAndOverdue() {
        // Even past the target date and paused, a reached goal is ACHIEVED.
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1000"), new BigDecimal("1200"), Goal.Type.SAVINGS,
                true, PAST, null, null, TODAY);
        assertEquals(GoalStatus.ACHIEVED, calc.status());
        assertEquals(new BigDecimal("100.00"), calc.percentageComplete());
    }

    @Test
    void overdueWhenPastTargetAndUnmet() {
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1000"), new BigDecimal("400"), Goal.Type.SAVINGS,
                false, PAST, null, null, TODAY);
        assertEquals(GoalStatus.OVERDUE, calc.status());
    }

    @Test
    void pausedOverridesDerivedStatus() {
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1000"), new BigDecimal("400"), Goal.Type.SAVINGS,
                true, FUTURE, null, null, TODAY);
        assertEquals(GoalStatus.PAUSED, calc.status());
    }

    @Test
    void estimatedCompletionProjectedFromVelocity() {
        // 600 saved over 30 days => 20/day; 600 remaining => ~30 more days from last contribution.
        LocalDate first = TODAY.minusDays(30);
        LocalDate last = TODAY.minusDays(1);
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1200"), new BigDecimal("600"), Goal.Type.SAVINGS,
                false, FUTURE, first, last, TODAY);
        assertEquals(LocalDate.of(2026, 8, 15), calc.estimatedCompletionDate());
    }

    @Test
    void noEstimatedCompletionWhenNothingSaved() {
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1200"), BigDecimal.ZERO, Goal.Type.SAVINGS,
                false, FUTURE, null, null, TODAY);
        assertNull(calc.estimatedCompletionDate());
    }

    @Test
    void noEstimatedCompletionWhenAlreadyAchieved() {
        GoalCalculation calc = GoalCalculator.evaluate(
                new BigDecimal("1000"), new BigDecimal("1000"), Goal.Type.SAVINGS,
                false, FUTURE, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), TODAY);
        assertNull(calc.estimatedCompletionDate());
    }
}
