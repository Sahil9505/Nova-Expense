package com.nova.finance.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * The single, pure source of goal math. Given a goal's target, current amount, pause
 * flag, target date, and contribution history, it derives the percentage complete,
 * remaining amount, derived {@link GoalStatus}, and a best-effort estimated completion
 * date. Kept free of persistence and web concerns so it is trivially unit-testable and
 * reusable by any future module that needs to reason about goal progress — the same
 * "single source of truth, no duplication" role {@code BudgetCalculator} plays for
 * budgets. (Goals intentionally do not reuse budget math: budgets measure spent-against-a
 * -limit over a window, while goals measure accumulated-progress-toward-a-target.)
 */
public final class GoalCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    /** Minimum average daily velocity (in target currency) before a completion date is projected. */
    private static final BigDecimal MIN_DAILY_VELOCITY = new BigDecimal("0.01");

    private GoalCalculator() {
    }

    /**
     * Derives a goal's progress.
     *
     * @param targetAmount   the goal's target (expected positive; non-positive is treated as never completable)
     * @param currentAmount  the maintained running total of contributions
     * @param type           the goal's type (used only to keep the signature aligned with the entity)
     * @param paused         whether the user has paused the goal (overrides derived status with PAUSED)
     * @param targetDate     the date the goal should be met by
     * @param firstContributionDate earliest contribution date, used to compute velocity (null if none)
     * @param lastContributionDate  latest contribution date (null if none)
     * @param referenceDate  "today", used for overdue detection and velocity
     */
    public static GoalCalculation evaluate(
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            Goal.Type type,
            boolean paused,
            LocalDate targetDate,
            LocalDate firstContributionDate,
            LocalDate lastContributionDate,
            LocalDate referenceDate) {

        BigDecimal safeTarget = targetAmount != null ? targetAmount : BigDecimal.ZERO;
        BigDecimal safeCurrent = currentAmount != null ? currentAmount : BigDecimal.ZERO;
        BigDecimal rawRemaining = safeTarget.subtract(safeCurrent).max(BigDecimal.ZERO);
        BigDecimal remaining = rawRemaining.signum() == 0
                ? BigDecimal.ZERO
                : rawRemaining.setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentage;
        if (safeTarget.signum() <= 0 || safeCurrent.signum() <= 0) {
            percentage = BigDecimal.ZERO;
        } else {
            percentage = safeCurrent.divide(safeTarget, 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
            // Display cap: a goal can be reported as at most 100% complete.
            if (percentage.compareTo(HUNDRED) > 0) {
                percentage = HUNDRED.setScale(2, RoundingMode.HALF_UP);
            }
        }

        boolean achieved = safeTarget.signum() > 0 && safeCurrent.compareTo(safeTarget) >= 0;
        GoalStatus status;
        if (achieved) {
            status = GoalStatus.ACHIEVED;
        } else if (paused) {
            status = GoalStatus.PAUSED;
        } else {
            boolean overdue = targetDate != null && referenceDate != null && referenceDate.isAfter(targetDate);
            status = overdue ? GoalStatus.OVERDUE : (safeCurrent.signum() > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED);
        }

        LocalDate estimatedCompletion = null;
        if (!achieved && safeTarget.signum() > 0) {
            estimatedCompletion = estimateCompletion(
                    safeTarget, safeCurrent, firstContributionDate, lastContributionDate, referenceDate);
        }

        return new GoalCalculation(percentage, remaining, status, estimatedCompletion);
    }

    /**
     * Projects the completion date from the average daily contribution velocity since
     * the first contribution. Returns null when there is no positive velocity yet
     * (e.g. nothing contributed, or the only contributions are today) so the UI can
     * show "on track" without a misleading date.
     */
    private static LocalDate estimateCompletion(
            BigDecimal target,
            BigDecimal current,
            LocalDate firstContributionDate,
            LocalDate lastContributionDate,
            LocalDate referenceDate) {
        if (firstContributionDate == null || referenceDate == null) {
            return null;
        }
        // Elapsed time since the first contribution started building progress.
        long daysElapsed = referenceDate.toEpochDay() - firstContributionDate.toEpochDay();
        if (daysElapsed <= 0) {
            return null; // contributions are too new to yield a stable rate
        }
        BigDecimal contributed = current;
        if (contributed.signum() <= 0) {
            return null;
        }
        BigDecimal dailyVelocity = contributed.divide(BigDecimal.valueOf(daysElapsed), 6, RoundingMode.HALF_UP);
        if (dailyVelocity.compareTo(MIN_DAILY_VELOCITY) < 0) {
            return null; // effectively stalled; don't project
        }
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);
        if (remaining.signum() <= 0) {
            return null;
        }
        BigDecimal daysRemaining = remaining.divide(dailyVelocity, 6, RoundingMode.HALF_UP);
        long days = daysRemaining.setScale(0, RoundingMode.UP).longValue();
        // Use the latest contribution as the anchor for "most recent progress".
        LocalDate anchor = lastContributionDate != null ? lastContributionDate : referenceDate;
        return anchor.plusDays(days);
    }
}
