package com.nova.finance.budget;

import com.nova.finance.AbstractFinanceApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetApiTest extends AbstractFinanceApiTest {

    private final String password = "sup3rSecret!";

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    @Test
    void createListAndFetch() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Groceries");
        body.put("amount", 500);
        body.put("period", "MONTHLY");
        body.put("startDate", "2026-01-01");

        MvcResult created = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Groceries"))
                .andExpect(jsonPath("$.data.amount").value(500))
                .andExpect(jsonPath("$.data.period").value("MONTHLY"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.category").doesNotExist())
                .andReturn();
        String id = parse(created).get("id").asText();

        mockMvc.perform(get("/api/budgets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/budgets/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void rejectsNonPositiveAmount() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Zero");
        body.put("amount", 0);
        body.put("period", "MONTHLY");
        body.put("startDate", "2026-01-01");
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsMissingPeriod() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "NoPeriod");
        body.put("amount", 100);
        body.put("startDate", "2026-01-01");
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void customPeriodRequiresBothDates() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Trip");
        body.put("amount", 2000);
        body.put("period", "CUSTOM");
        body.put("startDate", "2026-07-01");
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("BAD_REQUEST"));
    }

    @Test
    void customPeriodWithRangeSucceeds() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Trip");
        body.put("amount", 2000);
        body.put("period", "CUSTOM");
        body.put("startDate", "2026-07-01");
        body.put("endDate", "2026-07-31");
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endDate").value("2026-07-31"));
    }

    @Test
    void duplicateNameConflicts() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Rent");
        body.put("amount", 1500);
        body.put("period", "MONTHLY");
        body.put("startDate", "2026-01-01");
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("CONFLICT"));
    }

    @Test
    void scopedToOwnedCategory() throws Exception {
        String token = register(email(), password).get("accessToken");
        // Use a custom name so it doesn't collide with the categories seeded on registration.
        String categoryId = createCategory(token, "Dining Out", "EXPENSE");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Eating out");
        body.put("amount", 300);
        body.put("period", "MONTHLY");
        body.put("startDate", "2026-01-01");
        body.put("categoryId", categoryId);

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category.id").value(categoryId))
                .andExpect(jsonPath("$.data.category.name").value("Dining Out"));
    }

    @Test
    void rejectsAnotherUsersCategory() throws Exception {
        String ownerToken = register(email(), password).get("accessToken");
        String otherToken = register(email(), password).get("accessToken");
        String categoryId = createCategory(ownerToken, "Private", "EXPENSE");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Leak");
        body.put("amount", 100);
        body.put("period", "MONTHLY");
        body.put("startDate", "2026-01-01");
        body.put("categoryId", categoryId);

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void updateChangesFieldsAndActive() throws Exception {
        String token = register(email(), password).get("accessToken");
        String id = createBudget(token, budgetBody("Fuel", 200, "MONTHLY", "2026-01-01"));

        mockMvc.perform(patch("/api/budgets/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Transport", "amount", 250))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Transport"))
                .andExpect(jsonPath("$.data.amount").value(250));

        mockMvc.perform(patch("/api/budgets/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void deleteSoftDeactivates() throws Exception {
        String token = register(email(), password).get("accessToken");
        String id = createBudget(token, budgetBody("Streaming", 30, "MONTHLY", "2026-01-01"));

        mockMvc.perform(delete("/api/budgets/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        // Preserved but flagged inactive.
        mockMvc.perform(get("/api/budgets/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        // Hidden from the active-only view.
        mockMvc.perform(get("/api/budgets?active=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void userCannotAccessAnotherUsersBudget() throws Exception {
        String ownerToken = register(email(), password).get("accessToken");
        String ownerBudgetId = createBudget(ownerToken, budgetBody("Private", 100, "MONTHLY", "2026-01-01"));

        String otherToken = register(email(), password).get("accessToken");
        mockMvc.perform(get("/api/budgets/" + ownerBudgetId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("RESOURCE_NOT_FOUND"));
    }

    private Map<String, Object> budgetBody(String name, Number amount, String period, String startDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("amount", amount);
        body.put("period", period);
        body.put("startDate", startDate);
        return body;
    }
}
