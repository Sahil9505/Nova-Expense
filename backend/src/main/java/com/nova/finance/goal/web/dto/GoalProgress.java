package com.nova.finance.goal.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The intelligence derived for one goal: target, current, remaining, percentage
 * complete, derived status, and a best-effort estimated completion date. Purely
 * additive to the existing goal API — the {@link GoalResponse} shape is never changed.
 *
 * @param targetAmount            the goal's target
 * @param currentAmount           the maintained running total of contributions
 * @param remainingAmount         {@code target - current}, never negative
 * @param percentageComplete      {@code current / target * 100}, capped at 100
 * @param status                  NOT_STARTED | IN_PROGRESS | ACHIEVED | OVERDUE | PAUSED
 * @param estimatedCompletionDate projection from contribution velocity, or null
 */
public record GoalProgress(
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        BigDecimal percentageComplete,
        String status,
        LocalDate estimatedCompletionDate
) {
}
