package com.nova.ai;

import com.nova.finance.analytics.AnalyticsFilter;
import com.nova.finance.analytics.AnalyticsService;
import com.nova.finance.analytics.web.dto.AnalyticsOverviewResponse;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import com.nova.finance.goal.GoalService;
import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import com.nova.finance.receipt.ReceiptService;
import com.nova.finance.receipt.web.dto.ReceiptSummaryResponse;
import com.nova.user.UserRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Gathers exactly the data the detected intent needs, by delegating to Nova's
 * existing domain services. It performs <strong>no financial calculation</strong>
 * of its own — it only selects and forwards figures produced by Analytics, Budgets,
 * Goals, Receipts, and Transactions. This is the enforcement point of the Golden
 * Rule: the AI explains data, it never invents or recomputes it.
 *
 * <p>Each intent has a focused branch so the prompt sent to the model stays
 * minimal (see PERFORMANCE notes in the phase brief).</p>
 */
@Component
public class FinancialContextBuilder {

    private final AnalyticsService analyticsService;
    private final GoalService goalService;
    private final ReceiptService receiptService;
    private final UserRepository userRepository;

    public FinancialContextBuilder(
            AnalyticsService analyticsService,
            GoalService goalService,
            ReceiptService receiptService,
            UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.goalService = goalService;
        this.receiptService = receiptService;
        this.userRepository = userRepository;
    }

    public FinancialContext build(UUID userId, IntentType intent, PeriodResolver.Window period) {
        String currency = userRepository.findPreferredCurrencyById(userId);

        return switch (intent) {
            // Spending only needs the headline + category breakdown for the period.
            case SPENDING -> withOverview(userId, currency, period, false, false);
            // Cash-flow questions also benefit from the trend series.
            case CASH_FLOW -> withOverview(userId, currency, period, true, false);
            // Health / summary pull the broad picture including budget + goal standing.
            case FINANCIAL_HEALTH, GENERAL_SUMMARY -> withOverview(userId, currency, period, true, true);
            case COMPARISON -> withComparison(userId, currency, period);
            case BUDGET ->
                    new FinancialContext(currency, null, null, null, null,
                            budgetSummary(userId, currency), null, null, null, null);
            case GOALS ->
                    new FinancialContext(currency, null, null, null, null, null,
                            goalSummary(userId, currency), null, null, null);
            case RECEIPTS ->
                    new FinancialContext(currency, null, null, null, null, null, null,
                            recentReceipts(userId), null, null);
        };
    }

    private FinancialContext withOverview(UUID userId, String currency, PeriodResolver.Window period,
            boolean withCashFlow, boolean withBudgetAndGoals) {
        AnalyticsOverviewResponse overview = analyticsService.overview(
                AnalyticsFilter.forRange(userId, period.from(), period.to(), null, null), currency);
        return new FinancialContext(
                currency,
                overview,
                overview.spendingOverview(),
                withCashFlow ? overview.cashFlow() : null,
                overview.categoryAnalysis(),
                withBudgetAndGoals && overview.budgetAnalytics() != null
                        ? overview.budgetAnalytics().budgetSummary() : null,
                withBudgetAndGoals && overview.goalAnalytics() != null
                        ? overview.goalAnalytics().goalSummary() : null,
                null,
                null,
                null);
    }

    /**
     * Loads the current window and the immediately-preceding window of equal length
     * (e.g. this month vs. last month) so the model can explain a real change. The
     * previous window is derived from the given one — {@code [from - span, from)} —
     * and both figures come from {@link AnalyticsService}; the AI never computes the
     * delta itself.
     */
    private FinancialContext withComparison(UUID userId, String currency, PeriodResolver.Window current) {
        AnalyticsOverviewResponse currentOverview = analyticsService.overview(
                AnalyticsFilter.forRange(userId, current.from(), current.to(), null, null), currency);
        OffsetDateTime previousFrom = current.from().minusMonths(1);
        AnalyticsOverviewResponse previousOverview = analyticsService.overview(
                AnalyticsFilter.forRange(userId, previousFrom, current.from(), null, null), currency);
        return new FinancialContext(
                currency,
                currentOverview,
                currentOverview.spendingOverview(),
                currentOverview.cashFlow(),
                currentOverview.categoryAnalysis(),
                null, null, null, null,
                previousOverview);
    }

    private BudgetSummaryResponse budgetSummary(UUID userId, String currency) {
        return analyticsService.budgetAnalytics(userId, currency).budgetSummary();
    }

    private GoalSummaryResponse goalSummary(UUID userId, String currency) {
        return goalService.summary(userId, currency);
    }

    private List<ReceiptSummaryResponse> recentReceipts(UUID userId) {
        return receiptService.recent(userId, 10);
    }
}
