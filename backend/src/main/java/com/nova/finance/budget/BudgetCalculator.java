package com.nova.finance.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single, pure source of budget math. Given a planned amount, the spend, and the
 * configured thresholds, it derives remaining, percentage used, and {@link BudgetStatus}.
 * Kept free of persistence and web concerns so it is trivially unit-testable and
 * reusable by any future module that needs to classify budget health.
 */
public final class BudgetCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private BudgetCalculator() {
    }

    /**
     * Evaluates a budget's health.
     *
     * @param amount    the planned limit (expected positive; non-positive is treated as unlimited-safe)
     * @param spent     total spend for the period (null treated as zero, never negative-clamped)
     * @param thresholds configured warning/exceeded fractions
     */
    public static BudgetCalculation evaluate(BigDecimal amount, BigDecimal spent, BudgetProperties thresholds) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal safeSpent = spent != null ? spent : BigDecimal.ZERO;
        BigDecimal remaining = safeAmount.subtract(safeSpent);

        // A non-positive limit can't yield a meaningful ratio; report 0% and let status
        // reflect only whether anything was spent.
        if (safeAmount.signum() <= 0) {
            BudgetStatus status = safeSpent.signum() > 0 ? BudgetStatus.EXCEEDED : BudgetStatus.HEALTHY;
            return new BudgetCalculation(safeAmount, safeSpent, remaining, BigDecimal.ZERO, status);
        }

        BigDecimal ratio = safeSpent.divide(safeAmount, 6, RoundingMode.HALF_UP);
        BigDecimal percentageUsed = ratio.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
        BudgetStatus status = classify(ratio, thresholds);
        return new BudgetCalculation(safeAmount, safeSpent, remaining, percentageUsed, status);
    }

    private static BudgetStatus classify(BigDecimal ratio, BudgetProperties thresholds) {
        if (ratio.compareTo(thresholds.exceededThreshold()) >= 0) {
            return BudgetStatus.EXCEEDED;
        }
        if (ratio.compareTo(thresholds.warningThreshold()) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.HEALTHY;
    }
}
