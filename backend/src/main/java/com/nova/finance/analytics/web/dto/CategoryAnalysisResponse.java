package com.nova.finance.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spending and income broken down by category for the applied window. Reuses the same
 * {@code name / color / icon / amount} shape the dashboard category breakdown uses, so
 * the Analytics pie and the dashboard widget render identically.
 *
 * @param expenseTotal    total EXPENSE across all categories in the window
 * @param incomeTotal     total INCOME across all categories in the window
 * @param expenses        per-category expense totals, descending by amount
 * @param incomes         per-category income totals, descending by amount
 * @param topCategories   the highest-expense categories (subset of {@code expenses})
 * @param currency        the owner's preferred currency (for display)
 */
public record CategoryAnalysisResponse(
        BigDecimal expenseTotal,
        BigDecimal incomeTotal,
        List<CategoryBreakdownItem> expenses,
        List<CategoryBreakdownItem> incomes,
        List<CategoryBreakdownItem> topCategories,
        String currency
) {
}
