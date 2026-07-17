package com.nova.finance.goal.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The Goal overview for the dashboard. The header figures (total goals, active,
 * achieved, paused, overdue, total target, total saved, remaining, overall percent)
 * are computed in a single pass over the user's goals; {@code goals} carries each goal
 * with its derived progress so the "Goal Summary / Progress / Upcoming Deadlines /
 * Recently Completed" widgets can be composed client-side from one query.
 *
 * @param totalGoals      count of all goals (active + inactive)
 * @param activeGoals     count of goals with {@code active=true}
 * @param achievedGoals   goals currently at/over their target
 * @param pausedGoals     goals currently paused
 * @param overdueGoals    active, not achieved, past their target date
 * @param totalTarget     sum of target amounts across active goals
 * @param totalCurrent    sum of current amounts across active goals
 * @param totalRemaining  {@code totalTarget - totalCurrent} across active goals
 * @param overallPercent  {@code totalCurrent / totalTarget * 100} across active goals
 * @param currency        the owner's preferred currency (for display)
 * @param goals           every goal (active + inactive) with its derived progress
 */
public record GoalSummaryResponse(
        int totalGoals,
        int activeGoals,
        int achievedGoals,
        int pausedGoals,
        int overdueGoals,
        BigDecimal totalTarget,
        BigDecimal totalCurrent,
        BigDecimal totalRemaining,
        BigDecimal overallPercent,
        String currency,
        List<GoalWithProgress> goals
) {
}
