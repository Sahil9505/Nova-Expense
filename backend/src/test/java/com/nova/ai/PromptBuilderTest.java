package com.nova.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nova.finance.analytics.web.dto.AnalyticsOverviewResponse;
import com.nova.finance.analytics.web.dto.CashFlowPoint;
import com.nova.finance.analytics.web.dto.CashFlowResponse;
import com.nova.finance.analytics.web.dto.CategoryAnalysisResponse;
import com.nova.finance.analytics.web.dto.CategoryBreakdownItem;
import com.nova.finance.analytics.web.dto.Granularity;
import com.nova.finance.analytics.web.dto.SpendingOverviewResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    private FinancialContext sampleContext() {
        SpendingOverviewResponse spending = new SpendingOverviewResponse(
                new BigDecimal("5000.00"), new BigDecimal("3200.50"),
                new BigDecimal("1799.50"), new BigDecimal("36.0"), 42L, "USD");
        CategoryAnalysisResponse categories = new CategoryAnalysisResponse(
                new BigDecimal("3200.50"), BigDecimal.ZERO,
                List.of(new CategoryBreakdownItem("Food", "#fff", "utensils", new BigDecimal("900.00"), "EXPENSE")),
                List.of(), List.of(new CategoryBreakdownItem("Food", "#fff", "utensils", new BigDecimal("900.00"), "EXPENSE")),
                "USD");
        CashFlowResponse cashFlow = new CashFlowResponse(Granularity.MONTHLY, "USD",
                List.of(new CashFlowPoint("2026-07", "Jul", new BigDecimal("5000.00"), new BigDecimal("3200.50"), new BigDecimal("1799.50"))));
        AnalyticsOverviewResponse overview = new AnalyticsOverviewResponse(
                spending, cashFlow, categories, null, null,
                "USD", Instant.now(), OffsetDateTime.now(), OffsetDateTime.now().plusMonths(1), null, null);
        return new FinancialContext("USD", overview, spending, cashFlow, categories, null, null, null, null, null);
    }

    @Test
    void systemInstructionEnforcesGroundingRules() {
        String sys = builder.systemInstruction();
        assertTrue(sys.contains("Answer ONLY from the"), "must forbid outside knowledge");
        assertTrue(sys.contains("do not have enough information"), "must instruct honest fallback");
        assertTrue(sys.contains("another user's data"), "must forbid cross-user leakage");
        assertTrue(sys.contains("never reveal"), "must forbid leaking the system prompt");
    }

    @Test
    void contextDocumentIncludesOnlySuppliedFigures() {
        String doc = builder.buildContextDocument(sampleContext(), IntentType.SPENDING);
        assertTrue(doc.contains("FINANCIAL DATA"));
        assertTrue(doc.contains("5000.00 USD"), "income must appear");
        assertTrue(doc.contains("3200.50 USD"), "expenses must appear");
        assertTrue(doc.contains("Food"), "top category must appear");
        assertTrue(doc.contains("Spending"), "intent label must appear");
        assertFalse(doc.contains("BUDGET"), "no budget data should be present for a spending intent");
    }
}
