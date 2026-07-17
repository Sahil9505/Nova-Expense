package com.nova.finance.budget.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The Budget Intelligence overview for the dashboard and the Budgets summary strip.
 * The four header figures (active budgets, total budgeted, total spent, remaining)
 * are computed in a single pass over the user's budgets and their aggregated spending;
 * {@code budgets} carries the per-budget detail (with metrics) for building the
 * "near limit" and "recently exceeded" widgets on the client.
 *
 * @param activeBudgets     count of budgets with {@code active=true}
 * @param totalBudgeted     sum of the planned limits across active budgets
 * @param totalSpent        sum of spent amounts across active budgets (their current window)
 * @param totalRemaining    {@code totalBudgeted - totalSpent} across active budgets
 * @param currency          the owner's preferred currency (for display)
 * @param warningCount      active budgets currently in WARNING status
 * @param exceededCount     active budgets currently EXCEEDED
 * @param budgets          every budget (active + inactive) with its live metrics
 */
public record BudgetSummaryResponse(
        int activeBudgets,
        BigDecimal totalBudgeted,
        BigDecimal totalSpent,
        BigDecimal totalRemaining,
        String currency,
        int warningCount,
        int exceededCount,
        List<BudgetMetricsResponse> budgets
) {
}
