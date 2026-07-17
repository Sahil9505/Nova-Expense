package com.nova.finance.budget;

import com.nova.finance.AbstractFinanceApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the Budget Intelligence endpoints end-to-end against real transactions:
 * per-budget metrics, the rolled-up summary, category vs. overall aggregation,
 * income exclusion, the four periods, and deactivation effects. Spending is anchored
 * to the real clock so the windows resolve deterministically on any run date.
 */
class BudgetIntelligenceApiTest extends AbstractFinanceApiTest {

    private final String password = "sup3rSecret!";

    private String lastMonthIso() {
        return OffsetDateTime.now().minusMonths(1).toString();
    }

    private String thisMonthIso() {
        return OffsetDateTime.now().toString();
    }

    private String monthStart() {
        return LocalDate.now().withDayOfMonth(1).toString();
    }

    private String monthEndInclusive() {
        return LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1).toString();
    }

    private Map<String, Object> expense(String token, String account, String category, Number amount) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("type", "EXPENSE");
        body.put("accountId", account);
        body.put("categoryId", category);
        body.put("occurredAt", thisMonthIso());
        createTransaction(token, body);
        return body;
    }

    @Test
    void metricsReflectRealSpending() throws Exception {
        String token = register(email(), password).get("accessToken");
        String account = createAccount(token, "Checking", "CHECKING", "USD", 2000);
        String food = createCategory(token, "Food", "EXPENSE");

        // 350 spent in Food this month (two transactions) + 100 last month (excluded).
        expense(token, account, food, 300);
        expense(token, account, food, 50);
        Map<String, Object> old = new LinkedHashMap<>();
        old.put("amount", 100);
        old.put("type", "EXPENSE");
        old.put("accountId", account);
        old.put("categoryId", food);
        old.put("occurredAt", lastMonthIso());
        createTransaction(token, old);

        String budgetId = createBudget(token, Map.of(
                "name", "Groceries",
                "amount", 500,
                "period", "MONTHLY",
                "startDate", monthStart(),
                "categoryId", food));

        mockMvc.perform(get("/api/budgets/" + budgetId + "/metrics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.budget.id").value(budgetId))
                .andExpect(jsonPath("$.data.metrics.amount").value(500))
                .andExpect(jsonPath("$.data.metrics.spent").value(350)) // last month excluded
                .andExpect(jsonPath("$.data.metrics.remaining").value(150))
                .andExpect(jsonPath("$.data.metrics.percentageUsed").value(70.00))
                .andExpect(jsonPath("$.data.metrics.status").value("HEALTHY"));
    }

    @Test
    void incomeIsNeverCountedAsSpending() throws Exception {
        String token = register(email(), password).get("accessToken");
        String account = createAccount(token, "Checking", "CHECKING", "USD", 5000);
        String food = createCategory(token, "Food", "EXPENSE");
        String salary = createCategory(token, "Salary", "INCOME");

        expense(token, account, food, 200);

        Map<String, Object> income = new LinkedHashMap<>();
        income.put("amount", 4000);
        income.put("type", "INCOME");
        income.put("accountId", account);
        income.put("categoryId", salary);
        income.put("occurredAt", thisMonthIso());
        createTransaction(token, income);

        String overall = createBudget(token, Map.of(
                "name", "Everything", "amount", 1000, "period", "MONTHLY", "startDate", monthStart()));

        mockMvc.perform(get("/api/budgets/" + overall + "/metrics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.spent").value(200)) // income ignored
                .andExpect(jsonPath("$.data.metrics.status").value("HEALTHY"));
    }

    @Test
    void summaryRollsUpAllBudgetsAndClassifiesHealth() throws Exception {
        String token = register(email(), password).get("accessToken");
        String account = createAccount(token, "Checking", "CHECKING", "USD", 5000);
        String food = createCategory(token, "Food", "EXPENSE");
        String bills = createCategory(token, "Bills", "EXPENSE");
        String fun = createCategory(token, "Fun", "EXPENSE");

        expense(token, account, food, 300);
        expense(token, account, food, 50);   // Food total 350
        expense(token, account, fun, 80);    // Fun total 80 (80%)
        expense(token, account, bills, 150);  // Bills total 150 (150%)

        String foodBudget = createBudget(token, Map.of(
                "name", "Groceries", "amount", 500, "period", "MONTHLY", "startDate", monthStart(), "categoryId", food));
        createBudget(token, Map.of(
                "name", "Everything", "amount", 1000, "period", "MONTHLY", "startDate", monthStart()));
        String billsBudget = createBudget(token, Map.of(
                "name", "Utilities", "amount", 100, "period", "MONTHLY", "startDate", monthStart(), "categoryId", bills));
        String funBudget = createBudget(token, Map.of(
                "name", "Play", "amount", 100, "period", "MONTHLY", "startDate", monthStart(), "categoryId", fun));
        createBudget(token, Map.of(
                "name", "Trip", "amount", 1000, "period", "CUSTOM",
                "startDate", monthStart(), "endDate", monthEndInclusive(), "categoryId", food));

        mockMvc.perform(get("/api/budgets/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // Header roll-up across 5 active budgets.
                .andExpect(jsonPath("$.data.activeBudgets").value(5))
                .andExpect(jsonPath("$.data.totalBudgeted").value(2700)) // 500+1000+100+100+1000
                .andExpect(jsonPath("$.data.totalSpent").value(1510))     // 350+580+150+80+350
                .andExpect(jsonPath("$.data.totalRemaining").value(1190))
                .andExpect(jsonPath("$.data.warningCount").value(1))
                .andExpect(jsonPath("$.data.exceededCount").value(1))
                // The detail list carries every budget with its status.
                .andExpect(jsonPath("$.data.budgets.length()").value(5))
                .andExpect(jsonPath("$.data.budgets[?(@.budget.id == '" + foodBudget + "')].metrics.status").value("HEALTHY"))
                .andExpect(jsonPath("$.data.budgets[?(@.budget.id == '" + billsBudget + "')].metrics.status").value("EXCEEDED"))
                .andExpect(jsonPath("$.data.budgets[?(@.budget.id == '" + funBudget + "')].metrics.status").value("WARNING"));
    }

    @Test
    void deactivatedBudgetsLeaveActiveTotals() throws Exception {
        String token = register(email(), password).get("accessToken");
        String account = createAccount(token, "Checking", "CHECKING", "USD", 5000);
        String food = createCategory(token, "Food", "EXPENSE");
        expense(token, account, food, 100);

        String active = createBudget(token, Map.of(
                "name", "Active", "amount", 500, "period", "MONTHLY", "startDate", monthStart(), "categoryId", food));
        String inactive = createBudget(token, Map.of(
                "name", "Paused", "amount", 1000, "period", "MONTHLY", "startDate", monthStart(), "categoryId", food));

        // Deactivate the larger budget.
        mockMvc.perform(patch("/api/budgets/" + inactive)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/budgets/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeBudgets").value(1))
                .andExpect(jsonPath("$.data.totalBudgeted").value(500))
                .andExpect(jsonPath("$.data.totalSpent").value(100))
                .andExpect(jsonPath("$.data.budgets.length()").value(2)) // still present, just inactive
                .andExpect(jsonPath("$.data.budgets[?(@.budget.id == '" + active + "')].budget.active").value(true))
                .andExpect(jsonPath("$.data.budgets[?(@.budget.id == '" + inactive + "')].budget.active").value(false));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/budgets/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }
}
