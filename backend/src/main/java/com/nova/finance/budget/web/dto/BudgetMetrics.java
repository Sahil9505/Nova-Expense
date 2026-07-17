package com.nova.finance.budget.web.dto;

import com.nova.finance.budget.BudgetCalculation;

import java.math.BigDecimal;

/**
 * The intelligence derived for one budget: planned amount, spent, remaining,
 * percentage used, and health status. Purely additive to the existing budget API —
 * the {@link BudgetResponse} shape is never changed.
 *
 * @param amount         the budget's planned limit
 * @param spent          expense within the budget's period and scope
 * @param remaining      {@code amount - spent} (negative once over budget)
 * @param percentageUsed {@code spent / amount * 100}, two decimals
 * @param status         HEALTHY | WARNING | EXCEEDED
 */
public record BudgetMetrics(
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentageUsed,
        String status
) {
    public static BudgetMetrics from(BudgetCalculation calc) {
        return new BudgetMetrics(
                calc.amount(),
                calc.spent(),
                calc.remaining(),
                calc.percentageUsed(),
                calc.status().name());
    }
}
