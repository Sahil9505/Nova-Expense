package com.nova.finance.budget;

import java.math.BigDecimal;

/**
 * The intelligence derived for a single budget over its current period: how much was
 * planned, how much was spent, what remains, the percentage used, and the resulting
 * {@link BudgetStatus}. A pure, presentation-agnostic value object — the web layer
 * projects it into a DTO, but Analytics/Goals/AI can consume it directly.
 *
 * @param amount         the budget's planned limit
 * @param spent          total expense within the budget's period and scope
 * @param remaining      {@code amount - spent} (negative once over budget)
 * @param percentageUsed {@code spent / amount * 100}, scaled to two decimals
 * @param status         health classification from the configured thresholds
 */
public record BudgetCalculation(
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentageUsed,
        BudgetStatus status
) {
}
