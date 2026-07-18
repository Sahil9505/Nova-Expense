package com.nova.ai;

import com.nova.finance.analytics.web.dto.AnalyticsOverviewResponse;
import com.nova.finance.analytics.web.dto.CashFlowResponse;
import com.nova.finance.analytics.web.dto.CategoryAnalysisResponse;
import com.nova.finance.analytics.web.dto.SpendingOverviewResponse;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import com.nova.finance.receipt.web.dto.ReceiptSummaryResponse;
import com.nova.finance.transaction.web.dto.TransactionResponse;

import java.util.List;

/**
 * The slice of Nova's data the assistant is allowed to explain for one question.
 * The builder populates <em>only</em> the fields the detected intent needs, so the
 * prompt stays small and the model is never handed data it shouldn't use. Every
 * field here was produced by an existing domain service — the AI layer adds no
 * financial logic of its own.
 */
public record FinancialContext(
        String currency,
        AnalyticsOverviewResponse overview,        // spending / cash-flow / category (current period)
        SpendingOverviewResponse spending,          // convenience copy of overview.spendingOverview
        CashFlowResponse cashFlow,
        CategoryAnalysisResponse categories,
        BudgetSummaryResponse budgets,
        GoalSummaryResponse goals,
        List<ReceiptSummaryResponse> receipts,
        List<TransactionResponse> recentTransactions,
        AnalyticsOverviewResponse previousOverview  // prior period, populated only for COMPARISON
) {
}
