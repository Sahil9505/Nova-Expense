package com.nova.finance.analytics.web.dto;

import com.nova.finance.budget.web.dto.BudgetSummaryResponse;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Budget health for the Analytics view. Wraps the existing, reusable
 * {@link BudgetSummaryResponse} (which already computes active counts, total budgeted /
 * spent / remaining, warning/exceeded counts, and per-budget metrics in a single pass)
 * and adds two roll-ups that the Analytics page surfaces directly:
 *
 * <ul>
 *   <li>{@code healthDistribution} — how many active budgets fall in each
 *       HEALTHY / WARNING / EXCEEDED band.</li>
 *   <li>{@code budgetEfficiencyPct} — {@code totalSpent / totalBudgeted * 100} across
 *       active budgets (how much of the planned envelope was consumed).</li>
 * </ul>
 *
 * No budget math is re-derived here; the figures come straight from the Budget
 * Intelligence engine via {@code BudgetSummaryResponse}.
 *
 * @param budgetSummary        the reusable Budget Intelligence roll-up
 * @param healthDistribution   active-budget count keyed by status
 * @param budgetEfficiencyPct  spent ÷ budgeted across active budgets
 * @param currency             the owner's preferred currency (for display)
 */
public record BudgetAnalyticsResponse(
        BudgetSummaryResponse budgetSummary,
        Map<String, Long> healthDistribution,
        BigDecimal budgetEfficiencyPct,
        String currency
) {
}
