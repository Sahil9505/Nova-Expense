package com.nova.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nova.finance.analytics.AnalyticsFilter;
import com.nova.finance.analytics.AnalyticsService;
import com.nova.finance.analytics.web.dto.AnalyticsOverviewResponse;
import com.nova.finance.analytics.web.dto.BudgetAnalyticsResponse;
import com.nova.finance.analytics.web.dto.GoalAnalyticsResponse;
import com.nova.finance.analytics.web.dto.SpendingOverviewResponse;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import com.nova.finance.goal.GoalService;
import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import com.nova.finance.receipt.ReceiptService;
import com.nova.finance.receipt.web.dto.ReceiptSummaryResponse;
import com.nova.finance.transaction.TransactionService;
import com.nova.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class FinancialContextBuilderTest {

    private AnalyticsService analyticsService;
    private GoalService goalService;
    private ReceiptService receiptService;
    private UserRepository userRepository;
    private FinancialContextBuilder builder;

    private final UUID userId = UUID.randomUUID();
    private final PeriodResolver.Window period = new PeriodResolver.Window(
            OffsetDateTime.now().withDayOfMonth(1), OffsetDateTime.now().plusMonths(1));

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        goalService = mock(GoalService.class);
        receiptService = mock(ReceiptService.class);
        userRepository = mock(UserRepository.class);

        when(userRepository.findPreferredCurrencyById(userId)).thenReturn("USD");

        SpendingOverviewResponse spending = new SpendingOverviewResponse(
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, 1L, "USD");
        AnalyticsOverviewResponse overview = new AnalyticsOverviewResponse(
                spending, null, null, null, null, "USD", Instant.now(),
                OffsetDateTime.now(), OffsetDateTime.now(), null, null);
        when(analyticsService.overview(any(AnalyticsFilter.class), eq("USD"))).thenReturn(overview);

        BudgetSummaryResponse budgetSummary = new BudgetSummaryResponse(
                1, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "USD", 0, 0, List.of());
        when(analyticsService.budgetAnalytics(eq(userId), eq("USD")))
                .thenReturn(new BudgetAnalyticsResponse(budgetSummary, java.util.Map.of(), BigDecimal.ZERO, "USD"));

        GoalSummaryResponse goalSummary = new GoalSummaryResponse(
                1, 1, 0, 0, 0, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, "USD", List.of());
        when(goalService.summary(eq(userId), eq("USD"))).thenReturn(goalSummary);

        when(receiptService.recent(eq(userId), eq(10))).thenReturn(List.of());

        builder = new FinancialContextBuilder(analyticsService, goalService, receiptService, userRepository);
    }

    @Test
    void spendingIntentUsesAnalyticsOverviewOnly() {
        FinancialContext ctx = builder.build(userId, IntentType.SPENDING, period);
        assertNotNull(ctx.overview());
        assertNotNull(ctx.spending());
        assertNull(ctx.budgets());
        assertNull(ctx.goals());
        assertNull(ctx.receipts());
        verify(analyticsService).overview(any(AnalyticsFilter.class), eq("USD"));
    }

    @Test
    void budgetIntentUsesBudgetAnalyticsOnly() {
        FinancialContext ctx = builder.build(userId, IntentType.BUDGET, period);
        assertNull(ctx.overview());
        assertNotNull(ctx.budgets());
        assertNull(ctx.goals());
        verify(analyticsService).budgetAnalytics(eq(userId), eq("USD"));
    }

    @Test
    void goalsIntentUsesGoalServiceOnly() {
        FinancialContext ctx = builder.build(userId, IntentType.GOALS, period);
        assertNull(ctx.overview());
        assertNotNull(ctx.goals());
        assertNull(ctx.receipts());
        verify(goalService).summary(eq(userId), eq("USD"));
    }

    @Test
    void receiptsIntentUsesReceiptServiceOnly() {
        FinancialContext ctx = builder.build(userId, IntentType.RECEIPTS, period);
        assertNotNull(ctx.receipts());
        assertNull(ctx.overview());
        verify(receiptService).recent(eq(userId), eq(10));
    }

    @Test
    void currencyComesFromUserRepository() {
        FinancialContext ctx = builder.build(userId, IntentType.SPENDING, period);
        assertEquals("USD", ctx.currency());
        verify(userRepository).findPreferredCurrencyById(userId);
    }

    @Test
    void comparisonIntentLoadsCurrentAndPreviousPeriod() {
        FinancialContext ctx = builder.build(userId, IntentType.COMPARISON, period);
        assertNotNull(ctx.overview());
        // The prior window is derived from the given one and loaded from Analytics.
        assertNotNull(ctx.previousOverview());
        verify(analyticsService, org.mockito.Mockito.times(2))
                .overview(any(AnalyticsFilter.class), eq("USD"));
    }
}
