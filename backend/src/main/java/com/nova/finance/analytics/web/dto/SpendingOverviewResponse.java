package com.nova.finance.analytics.web.dto;

import java.math.BigDecimal;

/**
 * The headline figures for the applied window: how much came in, how much went out,
 * the resulting net cash flow, and the savings rate (net ÷ income, as a percentage).
 * Every value is derived from the user's real transactions — no estimates.
 *
 * @param income            sum of INCOME transactions in the window
 * @param expenses         sum of EXPENSE transactions in the window
 * @param netCashFlow      {@code income - expenses}
 * @param savingsRatePct   {@code netCashFlow / income * 100} (0 when there is no income)
 * @param transactionCount number of transactions (income + expense) in the window
 * @param currency         the owner's preferred currency (for display)
 */
public record SpendingOverviewResponse(
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal netCashFlow,
        BigDecimal savingsRatePct,
        long transactionCount,
        String currency
) {
}
