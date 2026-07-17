package com.nova.finance.goal;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The intelligence derived for a single goal: how far it has progressed toward its
 * target, what remains, the percentage complete, the derived {@link GoalStatus}, and
 * a best-effort estimated completion date. A pure, presentation-agnostic value
 * object — the web layer projects it into a DTO, but future Analytics/AI modules can
 * consume it directly (the Budget Intelligence engine does the same via its own
 * {@code BudgetCalculation}).
 *
 * @param percentageComplete {@code current / target * 100}, scaled to two decimals; capped at 100 for display
 * @param remaining          {@code target - current}, clamped so a goal is never "owed" past completion
 * @param status             derived lifecycle (NOT_STARTED / IN_PROGRESS / ACHIEVED / OVERDUE / PAUSED)
 * @param estimatedCompletionDate projection from average contribution velocity, or null when unknown
 */
public record GoalCalculation(
        BigDecimal percentageComplete,
        BigDecimal remaining,
        GoalStatus status,
        LocalDate estimatedCompletionDate
) {
}
