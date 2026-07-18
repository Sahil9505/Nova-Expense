package com.nova.finance.analytics;

import com.nova.finance.analytics.web.dto.AnalyticsExportRequest;
import com.nova.finance.analytics.web.dto.AnalyticsOverviewResponse;
import com.nova.finance.analytics.web.dto.BudgetAnalyticsResponse;
import com.nova.finance.analytics.web.dto.CashFlowPoint;
import com.nova.finance.analytics.web.dto.CashFlowResponse;
import com.nova.finance.analytics.web.dto.CategoryAnalysisResponse;
import com.nova.finance.analytics.web.dto.CategoryBreakdownItem;
import com.nova.finance.analytics.web.dto.ExportFormat;
import com.nova.finance.analytics.web.dto.GoalAnalyticsResponse;
import com.nova.finance.analytics.web.dto.Granularity;
import com.nova.finance.analytics.web.dto.SpendingOverviewResponse;
import com.nova.finance.budget.web.dto.BudgetMetrics;
import com.nova.finance.budget.web.dto.BudgetMetricsResponse;
import com.nova.finance.budget.web.dto.BudgetResponse;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for report generation. Builds an {@link AnalyticsOverviewResponse} by
 * hand (no Spring context) and asserts both the CSV and PDF renderers emit the real
 * figures and valid document markers.
 */
class ReportExportServiceTest {

    private final ReportExportService service = new ReportExportService();

    private AnalyticsOverviewResponse sample() {
        SpendingOverviewResponse spending = new SpendingOverviewResponse(
                new BigDecimal("2000.00"), new BigDecimal("500.00"), new BigDecimal("1500.00"),
                new BigDecimal("75.00"), 2, "USD");

        CashFlowResponse cashFlow = new CashFlowResponse(Granularity.MONTHLY, "USD", List.of(
                new CashFlowPoint("2026-07", "Jul", new BigDecimal("2000.00"), new BigDecimal("500.00"), new BigDecimal("1500.00"))));

        CategoryAnalysisResponse categories = new CategoryAnalysisResponse(
                new BigDecimal("500.00"), BigDecimal.ZERO,
                List.of(new CategoryBreakdownItem("Food", "#ff0000", "utensils", new BigDecimal("500.00"), "EXPENSE")),
                List.of(), List.of(), "USD");

        BudgetSummaryResponse budgetSummary = new BudgetSummaryResponse(
                1, new BigDecimal("500.00"), new BigDecimal("200.00"), new BigDecimal("300.00"),
                "USD", 0, 0,
                List.of(new BudgetMetricsResponse(
                        new BudgetResponse(java.util.UUID.randomUUID(), "Food Budget", null, new BigDecimal("500.00"),
                                "MONTHLY", null, true,
                                java.time.LocalDate.parse("2026-07-01"), null,
                                java.time.Instant.now(), java.time.Instant.now()),
                        new BudgetMetrics(new BigDecimal("500.00"), new BigDecimal("200.00"),
                                new BigDecimal("300.00"), new BigDecimal("40.00"), "HEALTHY"))));
        BudgetAnalyticsResponse budgets = new BudgetAnalyticsResponse(
                budgetSummary, Map.of("HEALTHY", 1L, "WARNING", 0L, "EXCEEDED", 0L),
                new BigDecimal("40.00"), "USD");

        GoalAnalyticsResponse goals = new GoalAnalyticsResponse(
                new com.nova.finance.goal.web.dto.GoalSummaryResponse(
                        1, 1, 0, 0, 0, new BigDecimal("1000.00"), BigDecimal.ZERO,
                        new BigDecimal("1000.00"), BigDecimal.ZERO, "USD", List.of()),
                List.of(), BigDecimal.ZERO, "USD");

        return new AnalyticsOverviewResponse(spending, cashFlow, categories, budgets, goals,
                "USD", Instant.now(),
                OffsetDateTime.parse("2026-07-01T00:00:00Z"), OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                null, null);
    }

    private AnalyticsExportRequest request(ExportFormat format) {
        return new AnalyticsExportRequest(format, java.util.UUID.randomUUID(),
                OffsetDateTime.parse("2026-07-01T00:00:00Z"), OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                null, null);
    }

    @Test
    void csvContainsAllSections() {
        byte[] csv = service.export(sample(), request(ExportFormat.CSV));
        String text = new String(csv);
        assertThat(text).contains("Nova Analytics Report");
        assertThat(text).contains("SPENDING OVERVIEW");
        assertThat(text).contains("CASH FLOW");
        assertThat(text).contains("BUDGET ANALYTICS");
        assertThat(text).contains("GOAL ANALYTICS");
        assertThat(text).contains("Food");
        assertThat(text).contains("2000.00 USD");
    }

    @Test
    void pdfIsValidDocument() {
        byte[] pdf = service.export(sample(), request(ExportFormat.PDF));
        assertThat(pdf.length).isGreaterThan(4);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void contentTypesAndExtensionsAreCorrect() {
        assertThat(service.contentType(ExportFormat.CSV)).isEqualTo("text/csv");
        assertThat(service.contentType(ExportFormat.PDF)).isEqualTo("application/pdf");
        assertThat(service.fileExtension(ExportFormat.CSV)).isEqualTo("csv");
        assertThat(service.fileExtension(ExportFormat.PDF)).isEqualTo("pdf");
    }
}
