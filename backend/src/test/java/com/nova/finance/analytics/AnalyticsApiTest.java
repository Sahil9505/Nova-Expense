package com.nova.finance.analytics;

import com.nova.finance.AbstractFinanceApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the Analytics API end-to-end against the in-memory H2 database. Verifies
 * aggregation correctness, filter scoping, reuse of the Budget/Goal engines, and that
 * report exports stream real data for both CSV and PDF.
 */
class AnalyticsApiTest extends AbstractFinanceApiTest {

    private final String password = "sup3rSecret!";

    private String categoryId(String token, String name, String type) throws Exception {
        var list = mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        for (var node : parse(list)) {
            if (name.equals(node.get("name").asText()) && type.equals(node.get("type").asText())) {
                return node.get("id").asText();
            }
        }
        throw new IllegalStateException("category not found: " + name);
    }

    @Test
    void overviewReflectsRealTransactions() throws Exception {
        String token = register(email(), password).get("accessToken");
        createAccount(token, "Checking", "CHECKING", "USD", 1000);
        String salary = categoryId(token, "Salary", "INCOME");
        String food = categoryId(token, "Food", "EXPENSE");
        String main = createAccount(token, "Main", "CHECKING", "USD", 0);

        String july = OffsetDateTime.parse("2026-07-10T12:00:00Z").toString();
        String june = OffsetDateTime.parse("2026-06-15T12:00:00Z").toString();
        createTransaction(token, Map.of("amount", 2000, "type", "INCOME",
                "accountId", main, "categoryId", salary, "occurredAt", july));
        createTransaction(token, Map.of("amount", 500, "type", "EXPENSE",
                "accountId", main, "categoryId", food, "occurredAt", july));
        createTransaction(token, Map.of("amount", 100, "type", "EXPENSE",
                "accountId", main, "categoryId", food, "occurredAt", june));

        mockMvc.perform(get("/api/analytics/overview?period=monthly").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spendingOverview.income").value(2000))
                .andExpect(jsonPath("$.data.spendingOverview.expenses").value(500))
                .andExpect(jsonPath("$.data.spendingOverview.netCashFlow").value(1500))
                .andExpect(jsonPath("$.data.spendingOverview.savingsRatePct").value(75.00))
                .andExpect(jsonPath("$.data.spendingOverview.transactionCount").value(2))
                .andExpect(jsonPath("$.data.categoryAnalysis.expenses[?(@.name == 'Food')]").exists())
                .andExpect(jsonPath("$.data.cashFlow.points.length()").value(1))
                .andExpect(jsonPath("$.data.cashFlow.points[0].income").value(2000))
                .andExpect(jsonPath("$.data.cashFlow.points[0].expenses").value(500))
                .andExpect(jsonPath("$.data.budgetAnalytics.budgetSummary").exists())
                .andExpect(jsonPath("$.data.goalAnalytics.goalSummary").exists());
    }

    @Test
    void accountFilterScopesAggregation() throws Exception {
        String token = register(email(), password).get("accessToken");
        String checking = createAccount(token, "Checking", "CHECKING", "USD", 1000);
        String savings = createAccount(token, "Savings", "SAVINGS", "USD", 500);
        String food = categoryId(token, "Food", "EXPENSE");
        String now = OffsetDateTime.parse("2026-07-10T12:00:00Z").toString();

        createTransaction(token, Map.of("amount", 1000, "type", "EXPENSE",
                "accountId", checking, "categoryId", food, "occurredAt", now));
        createTransaction(token, Map.of("amount", 300, "type", "EXPENSE",
                "accountId", savings, "categoryId", food, "occurredAt", now));

        mockMvc.perform(get("/api/analytics/overview?period=monthly&accountId=" + checking)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spendingOverview.expenses").value(1000));
    }

    @Test
    void categoryFilterScopesAggregation() throws Exception {
        String token = register(email(), password).get("accessToken");
        createAccount(token, "Checking", "CHECKING", "USD", 1000);
        String food = categoryId(token, "Food", "EXPENSE");
        String now = OffsetDateTime.parse("2026-07-10T12:00:00Z").toString();

        createTransaction(token, Map.of("amount", 400, "type", "EXPENSE", "accountId", createAccount(token, "A", "CHECKING", "USD", 0), "categoryId", food, "occurredAt", now));
        createTransaction(token, Map.of("amount", 600, "type", "EXPENSE", "accountId", createAccount(token, "B", "CHECKING", "USD", 0), "categoryId", food, "occurredAt", now));

        mockMvc.perform(get("/api/analytics/overview?period=monthly&categoryId=" + food)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spendingOverview.expenses").value(1000))
                .andExpect(jsonPath("$.data.categoryAnalysis.expenses.length()").value(1));
    }

    @Test
    void budgetsAndGoalsReuseExistingEngines() throws Exception {
        String token = register(email(), password).get("accessToken");
        createAccount(token, "Checking", "CHECKING", "USD", 1000);
        String food = categoryId(token, "Food", "EXPENSE");
        String now = OffsetDateTime.parse("2026-07-10T12:00:00Z").toString();
        createTransaction(token, Map.of("amount", 200, "type", "EXPENSE", "accountId", createAccount(token, "X", "CHECKING", "USD", 0), "categoryId", food, "occurredAt", now));
        String start = java.time.LocalDate.parse("2026-07-01").toString();
        createBudget(token, Map.of("name", "Food Budget", "amount", 500, "period", "MONTHLY", "categoryId", food, "startDate", start));
        createGoal(token, Map.of("name", "Vacation", "type", "SAVINGS", "targetAmount", 1000, "targetDate", "2026-12-31"));

        mockMvc.perform(get("/api/analytics/budgets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.budgetSummary.activeBudgets").value(1))
                .andExpect(jsonPath("$.data.budgetSummary.totalSpent").value(200))
                .andExpect(jsonPath("$.data.budgetEfficiencyPct").value(40.00))
                .andExpect(jsonPath("$.data.healthDistribution.WARNING").value(0));

        mockMvc.perform(get("/api/analytics/goals").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goalSummary.activeGoals").value(1))
                .andExpect(jsonPath("$.data.goalSummary.totalTarget").value(1000))
                .andExpect(jsonPath("$.data.upcomingDeadlines.length()").value(1));
    }

    @Test
    void exportReturnsCsvAndPdf() throws Exception {
        String token = register(email(), password).get("accessToken");
        createAccount(token, "Checking", "CHECKING", "USD", 1000);
        String salary = categoryId(token, "Salary", "INCOME");
        String food = categoryId(token, "Food", "EXPENSE");
        String now = OffsetDateTime.parse("2026-07-10T12:00:00Z").toString();
        createTransaction(token, Map.of("amount", 1000, "type", "INCOME", "accountId", createAccount(token, "M", "CHECKING", "USD", 0), "categoryId", salary, "occurredAt", now));
        createTransaction(token, Map.of("amount", 200, "type", "EXPENSE", "accountId", createAccount(token, "N", "CHECKING", "USD", 0), "categoryId", food, "occurredAt", now));

        String csv = mockMvc.perform(post("/api/analytics/reports/export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("format", "CSV"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("Nova Analytics Report"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("SPENDING OVERVIEW"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("Food"));

        byte[] pdf = mockMvc.perform(post("/api/analytics/reports/export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("format", "PDF"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(pdf.length > 4);
        org.junit.jupiter.api.Assertions.assertEquals("%PDF", new String(pdf, 0, 4));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }
}
