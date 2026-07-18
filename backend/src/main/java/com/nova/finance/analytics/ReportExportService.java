package com.nova.finance.analytics;

import com.nova.finance.analytics.web.dto.AnalyticsExportRequest;
import com.nova.finance.analytics.web.dto.AnalyticsOverviewResponse;
import com.nova.finance.analytics.web.dto.BudgetAnalyticsResponse;
import com.nova.finance.analytics.web.dto.CategoryAnalysisResponse;
import com.nova.finance.analytics.web.dto.CashFlowResponse;
import com.nova.finance.analytics.web.dto.ExportFormat;
import com.nova.finance.analytics.web.dto.GoalAnalyticsResponse;
import com.nova.finance.analytics.web.dto.SpendingOverviewResponse;
import com.nova.finance.budget.web.dto.BudgetSummaryResponse;
import com.nova.finance.budget.web.dto.BudgetMetricsResponse;
import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import com.nova.finance.goal.web.dto.GoalWithProgress;
import com.opencsv.CSVWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Generates downloadable analytics reports (CSV and PDF) from an already-computed
 * {@link AnalyticsOverviewResponse}. Reports are produced from the Analytics domain —
 * never from UI state — and honour the same {@link AnalyticsFilter} the snapshot was
 * built with, so the export always matches what the user sees on screen.
 *
 * <p>Both formats render only real figures; no statistics are synthesized. The CSV is a
 * flat, spreadsheet-friendly document with clearly delimited sections; the PDF is a
 * clean, tabular report using OpenPDF (no external fonts or binaries).</p>
 */
@Service
public class ReportExportService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    /** Formats an Instant for the report header (Instant has no year/month fields on its own). */
    private String formatInstant(java.time.Instant instant) {
        if (instant == null) {
            return "";
        }
        return TS.format(instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
    }

    /** Bytes of the requested report format for the given snapshot. */
    public byte[] export(AnalyticsOverviewResponse overview, AnalyticsExportRequest request) {
        if (request.format() == ExportFormat.PDF) {
            return toPdf(overview, request);
        }
        return toCsv(overview, request).getBytes(StandardCharsets.UTF_8);
    }

    public String contentType(ExportFormat format) {
        return format == ExportFormat.PDF ? "application/pdf" : "text/csv";
    }

    public String fileExtension(ExportFormat format) {
        return format == ExportFormat.PDF ? "pdf" : "csv";
    }

    // -----------------------------------------------------------------------
    // CSV
    // -----------------------------------------------------------------------

    private String toCsv(AnalyticsOverviewResponse o, AnalyticsExportRequest req) {
        StringWriter sink = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sink)) {
            writer.writeNext(new String[] { "Nova Analytics Report" });
            writer.writeNext(new String[] { "Generated", formatInstant(o.generatedAt()) });
            writer.writeNext(filterLines(req));
            writer.writeNext(new String[] {});

            SpendingOverviewResponse s = o.spendingOverview();
            writer.writeNext(new String[] { "SPENDING OVERVIEW" });
            writer.writeNext(new String[] { "Income", money(s.income(), o.currency()) });
            writer.writeNext(new String[] { "Expenses", money(s.expenses(), o.currency()) });
            writer.writeNext(new String[] { "Net Cash Flow", money(s.netCashFlow(), o.currency()) });
            writer.writeNext(new String[] { "Savings Rate %", s.savingsRatePct().toPlainString() });
            writer.writeNext(new String[] { "Transactions", String.valueOf(s.transactionCount()) });
            writer.writeNext(new String[] {});

            CashFlowResponse cf = o.cashFlow();
            writer.writeNext(new String[] { "CASH FLOW (" + cf.granularity() + ")" });
            writer.writeNext(new String[] { "Period", "Income", "Expenses", "Net" });
            for (var p : cf.points()) {
                writer.writeNext(new String[] { p.label(), money(p.income(), o.currency()),
                        money(p.expenses(), o.currency()), money(p.net(), o.currency()) });
            }
            writer.writeNext(new String[] {});

            CategoryAnalysisResponse cat = o.categoryAnalysis();
            writer.writeNext(new String[] { "CATEGORY BREAKDOWN — EXPENSES" });
            writer.writeNext(new String[] { "Category", "Amount", "Type" });
            for (var c : cat.expenses()) {
                writer.writeNext(new String[] { c.name(), money(c.amount(), o.currency()), c.type() });
            }
            writer.writeNext(new String[] { "CATEGORY BREAKDOWN — INCOME" });
            for (var c : cat.incomes()) {
                writer.writeNext(new String[] { c.name(), money(c.amount(), o.currency()), c.type() });
            }
            writer.writeNext(new String[] {});

            writeBudgetsCsv(writer, o.budgetAnalytics(), o.currency());
            writeGoalsCsv(writer, o.goalAnalytics(), o.currency());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build CSV report", e);
        }
        return sink.toString();
    }

    private void writeBudgetsCsv(CSVWriter writer, BudgetAnalyticsResponse b, String currency) {
        BudgetSummaryResponse s = b.budgetSummary();
        writer.writeNext(new String[] { "BUDGET ANALYTICS" });
        writer.writeNext(new String[] { "Active Budgets", String.valueOf(s.activeBudgets()) });
        writer.writeNext(new String[] { "Total Budgeted", money(s.totalBudgeted(), currency) });
        writer.writeNext(new String[] { "Total Spent", money(s.totalSpent(), currency) });
        writer.writeNext(new String[] { "Total Remaining", money(s.totalRemaining(), currency) });
        writer.writeNext(new String[] { "Efficiency %", b.budgetEfficiencyPct().toPlainString() });
        writer.writeNext(new String[] { "Healthy", String.valueOf(b.healthDistribution().getOrDefault("HEALTHY", 0L)) });
        writer.writeNext(new String[] { "Warning", String.valueOf(b.healthDistribution().getOrDefault("WARNING", 0L)) });
        writer.writeNext(new String[] { "Exceeded", String.valueOf(b.healthDistribution().getOrDefault("EXCEEDED", 0L)) });
        writer.writeNext(new String[] { "Budget", "Amount", "Spent", "Remaining", "Used %", "Status" });
        for (BudgetMetricsResponse m : s.budgets()) {
            writer.writeNext(new String[] {
                    m.budget().name(),
                    money(m.metrics().amount(), currency),
                    money(m.metrics().spent(), currency),
                    money(m.metrics().remaining(), currency),
                    m.metrics().percentageUsed().toPlainString(),
                    m.metrics().status()
            });
        }
        writer.writeNext(new String[] {});
    }

    private void writeGoalsCsv(CSVWriter writer, GoalAnalyticsResponse g, String currency) {
        GoalSummaryResponse s = g.goalSummary();
        writer.writeNext(new String[] { "GOAL ANALYTICS" });
        writer.writeNext(new String[] { "Active Goals", String.valueOf(s.activeGoals()) });
        writer.writeNext(new String[] { "Achieved Goals", String.valueOf(s.achievedGoals()) });
        writer.writeNext(new String[] { "Total Target", money(s.totalTarget(), currency) });
        writer.writeNext(new String[] { "Total Saved", money(s.totalCurrent(), currency) });
        writer.writeNext(new String[] { "Contribution Total", money(g.contributionTotal(), currency) });
        writer.writeNext(new String[] { "Overall %", s.overallPercent().toPlainString() });
        writer.writeNext(new String[] { "Goal", "Target", "Current", "Status", "Target Date" });
        for (GoalWithProgress p : s.goals()) {
            writer.writeNext(new String[] {
                    p.goal().name(),
                    money(p.goal().targetAmount(), currency),
                    money(p.progress().currentAmount(), currency),
                    p.progress().status(),
                    p.goal().targetDate() != null ? DATE.format(p.goal().targetDate()) : ""
            });
        }
        writer.writeNext(new String[] {});
    }

    // -----------------------------------------------------------------------
    // PDF
    // -----------------------------------------------------------------------

    private byte[] toPdf(AnalyticsOverviewResponse o, AnalyticsExportRequest req) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("Nova Analytics Report", title));
            document.add(new Paragraph("Generated " + formatInstant(o.generatedAt()), body));
            String[] meta = filterLines(req);
            for (int i = 0; i + 1 < meta.length; i += 2) {
                document.add(new Paragraph(meta[i] + ": " + meta[i + 1], body));
            }
            document.add(new Paragraph(" ", body));

            SpendingOverviewResponse s = o.spendingOverview();
            document.add(new Paragraph("Spending Overview", h2));
            document.add(table(new String[] { "Income", "Expenses", "Net", "Savings %", "Transactions" },
                    new String[] {
                            money(s.income(), o.currency()),
                            money(s.expenses(), o.currency()),
                            money(s.netCashFlow(), o.currency()),
                            s.savingsRatePct().toPlainString(),
                            String.valueOf(s.transactionCount())
                    }));

            CashFlowResponse cf = o.cashFlow();
            document.add(new Paragraph("Cash Flow (" + cf.granularity() + ")", h2));
            List<String[]> cfRows = new java.util.ArrayList<>();
            for (var p : cf.points()) {
                cfRows.add(new String[] { p.label(), money(p.income(), o.currency()),
                        money(p.expenses(), o.currency()), money(p.net(), o.currency()) });
            }
            document.add(table(new String[] { "Period", "Income", "Expenses", "Net" },
                    cfRows.toArray(new String[0][])));

            CategoryAnalysisResponse cat = o.categoryAnalysis();
            document.add(new Paragraph("Top Spending Categories", h2));
            List<String[]> catRows = new java.util.ArrayList<>();
            for (var c : cat.topCategories()) {
                catRows.add(new String[] { c.name(), money(c.amount(), o.currency()) });
            }
            document.add(table(new String[] { "Category", "Amount" }, catRows.toArray(new String[0][])));

            BudgetAnalyticsResponse b = o.budgetAnalytics();
            document.add(new Paragraph("Budget Analytics", h2));
            document.add(table(new String[] { "Active", "Budgeted", "Spent", "Remaining", "Efficiency %" },
                    new String[] {
                            String.valueOf(b.budgetSummary().activeBudgets()),
                            money(b.budgetSummary().totalBudgeted(), o.currency()),
                            money(b.budgetSummary().totalSpent(), o.currency()),
                            money(b.budgetSummary().totalRemaining(), o.currency()),
                            b.budgetEfficiencyPct().toPlainString()
                    }));

            GoalAnalyticsResponse g = o.goalAnalytics();
            document.add(new Paragraph("Goal Analytics", h2));
            document.add(table(new String[] { "Active", "Achieved", "Target", "Saved", "Overall %" },
                    new String[] {
                            String.valueOf(g.goalSummary().activeGoals()),
                            String.valueOf(g.goalSummary().achievedGoals()),
                            money(g.goalSummary().totalTarget(), o.currency()),
                            money(g.goalSummary().totalCurrent(), o.currency()),
                            g.goalSummary().overallPercent().toPlainString()
                    }));

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to build PDF report", e);
        }
        return out.toByteArray();
    }

    private PdfPTable table(String[] headers, String[]... rows) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(8);
        Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font cell = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (String header : headers) {
            Paragraph p = new Paragraph(header, head);
            p.setAlignment(Element.ALIGN_LEFT);
            table.addCell(p);
        }
        for (String[] row : rows) {
            for (String value : row) {
                table.addCell(new Phrase(value == null ? "" : value, cell));
            }
        }
        return table;
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private String[] filterLines(AnalyticsExportRequest req) {
        OffsetDateTime from = req.from();
        OffsetDateTime to = req.to();
        String range = (from != null && to != null)
                ? DATE.format(from) + " → " + DATE.format(to)
                : "last 12 months";
        String account = req.accountId() != null ? req.accountId().toString() : "all";
        String category = req.categoryId() != null ? req.categoryId().toString() : "all";
        return new String[] { "Period", range, "Account", account, "Category", category };
    }

    private String money(BigDecimal value, String currency) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + (currency == null ? "" : currency);
    }
}
