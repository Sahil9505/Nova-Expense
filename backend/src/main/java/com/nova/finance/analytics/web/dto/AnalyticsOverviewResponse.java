package com.nova.finance.analytics.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The complete Analytics payload for the applied filter. Composed by
 * {@code AnalyticsService.overview(...)} in a single read-only pass over the user's
 * transactions (plus the reused Budget and Goal engines), so the Analytics page, the
 * dashboard widgets, and report exports all consume one consistent, real-data snapshot.
 *
 * @param spendingOverview headline income / expenses / net / savings rate
 * @param cashFlow        income-vs-expense trend, bucketed by granularity
 * @param categoryAnalysis spending and income by category
 * @param budgetAnalytics budget health (current period) on top of BudgetSummaryResponse
 * @param goalAnalytics   goal progress (current period) on top of GoalSummaryResponse
 * @param currency        the owner's preferred currency (for display)
 * @param generatedAt    server instant the snapshot was produced
 * @param appliedFrom     the inclusive start of the window the report respects
 * @param appliedTo       the exclusive end of the window the report respects
 * @param accountId       optional account filter applied (null = all accounts)
 * @param categoryId      optional category filter applied (null = all categories)
 */
public record AnalyticsOverviewResponse(
        SpendingOverviewResponse spendingOverview,
        CashFlowResponse cashFlow,
        CategoryAnalysisResponse categoryAnalysis,
        BudgetAnalyticsResponse budgetAnalytics,
        GoalAnalyticsResponse goalAnalytics,
        String currency,
        Instant generatedAt,
        OffsetDateTime appliedFrom,
        OffsetDateTime appliedTo,
        UUID accountId,
        UUID categoryId
) {
}
