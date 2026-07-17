package com.nova.finance.goal;

import com.nova.finance.AbstractFinanceApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalContributionIntegrationTest extends AbstractFinanceApiTest {

    private final String password = "sup3rSecret!";

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @BeforeEach
    void cleanGoals() {
        goalContributionRepository.deleteAll();
        goalContributionRepository.flush();
        goalRepository.deleteAll();
        goalRepository.flush();
    }

    @Test
    void contributionIncrementsCurrentAmountAndRecordsHistory() throws Exception {
        String token = register(email(), password).get("accessToken");
        String id = createGoal(token, goalBody("House Deposit", "SAVINGS", 10000, "2027-01-01"));

        mockMvc.perform(post("/api/goals/" + id + "/contributions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contribution(500, "First lump", "2026-07-01"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goal.currentAmount").value(500))
                .andExpect(jsonPath("$.data.goal.progress.percentageComplete").value(5.00))
                .andExpect(jsonPath("$.data.goal.progress.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.contributions.length()").value(1))
                .andExpect(jsonPath("$.data.contributions[0].amount").value(500));

        // A second contribution accumulates and is capped at the target.
        mockMvc.perform(post("/api/goals/" + id + "/contributions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contribution(9700, "Top up", "2026-07-10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goal.currentAmount").value(10000))
                .andExpect(jsonPath("$.data.goal.progress.percentageComplete").value(100.00))
                .andExpect(jsonPath("$.data.goal.progress.status").value("ACHIEVED"))
                .andExpect(jsonPath("$.data.contributions.length()").value(2));

        // The running total never exceeds the target even with extra contributions.
        mockMvc.perform(post("/api/goals/" + id + "/contributions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contribution(500, "Overflow", "2026-07-11"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goal.currentAmount").value(10000));
    }

    @Test
    void rejectsNegativeContribution() throws Exception {
        String token = register(email(), password).get("accessToken");
        String id = createGoal(token, goalBody("Trip", "CUSTOM", 2000, "2026-12-31"));
        mockMvc.perform(post("/api/goals/" + id + "/contributions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contribution(-10, "Nope", "2026-07-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsContributionToInactiveGoal() throws Exception {
        String token = register(email(), password).get("accessToken");
        String id = createGoal(token, goalBody("Paused Goal", "SAVINGS", 1000, "2026-12-31"));
        mockMvc.perform(patch("/api/goals/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/goals/" + id + "/contributions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contribution(50, "Late", "2026-07-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("BAD_REQUEST"));
    }

    @Test
    void detailReturnsContributionHistoryForOwnerOnly() throws Exception {
        String ownerToken = register(email(), password).get("accessToken");
        String id = createGoal(ownerToken, goalBody("Bike", "SAVINGS", 800, "2026-12-31"));
        mockMvc.perform(post("/api/goals/" + id + "/contributions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contribution(200, "Saved", "2026-06-15"))))
                .andExpect(status().isOk());

        String otherToken = register(email(), password).get("accessToken");
        mockMvc.perform(get("/api/goals/" + id).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/goals/" + id).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contributions.length()").value(1))
                .andExpect(jsonPath("$.data.contributions[0].note").value("Saved"));
    }

    private Map<String, Object> goalBody(String name, String type, Number target, String targetDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", type);
        body.put("targetAmount", target);
        body.put("targetDate", targetDate);
        return body;
    }

    private Map<String, Object> contribution(Number amount, String note, String date) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("note", note);
        body.put("contributedAt", date);
        return body;
    }
}
