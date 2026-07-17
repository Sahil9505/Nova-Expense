package com.nova.finance.goal;

import com.nova.finance.AbstractFinanceApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

class GoalApiTest extends AbstractFinanceApiTest {

    private final String password = "sup3rSecret!";

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @BeforeEach
    void cleanGoals() {
        // Keep goal tests isolated; the shared H2 database is reused for the whole run.
        goalContributionRepository.deleteAll();
        goalContributionRepository.flush();
        goalRepository.deleteAll();
        goalRepository.flush();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    @Test
    void createListAndFetch() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = goalBody("Emergency Fund", "SAVINGS", 5000, "2026-12-31");

        MvcResult created = mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Emergency Fund"))
                .andExpect(jsonPath("$.data.type").value("SAVINGS"))
                .andExpect(jsonPath("$.data.targetAmount").value(5000))
                .andExpect(jsonPath("$.data.currentAmount").value(0))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.paused").value(false))
                .andExpect(jsonPath("$.data.progress.status").value("NOT_STARTED"))
                .andReturn();
        String id = parse(created).get("id").asText();

        mockMvc.perform(get("/api/goals").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/goals/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goal.id").value(id));
    }

    @Test
    void rejectsNonPositiveTarget() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = goalBody("Zero", "SAVINGS", 0, "2026-12-31");
        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsMissingType() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "NoType");
        body.put("targetAmount", 1000);
        body.put("targetDate", "2026-12-31");
        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsMissingTargetDate() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "NoDate");
        body.put("type", "SAVINGS");
        body.put("targetAmount", 1000);
        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void duplicateNameConflicts() throws Exception {
        String token = register(email(), password).get("accessToken");
        Map<String, Object> body = goalBody("Vacation", "SAVINGS", 2000, "2026-12-31");
        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("CONFLICT"));
    }

    @Test
    void updateChangesFieldsAndLifecycle() throws Exception {
        String token = register(email(), password).get("accessToken");
        String id = createGoal(token, goalBody("New Car", "SAVINGS", 20000, "2027-01-01"));

        mockMvc.perform(patch("/api/goals/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Used Car", "targetAmount", 12000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Used Car"))
                .andExpect(jsonPath("$.data.targetAmount").value(12000));

        mockMvc.perform(patch("/api/goals/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("paused", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paused").value(true))
                .andExpect(jsonPath("$.data.progress.status").value("PAUSED"));

        mockMvc.perform(patch("/api/goals/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void deleteSoftDeactivatesAndHidesFromActiveView() throws Exception {
        String token = register(email(), password).get("accessToken");
        String id = createGoal(token, goalBody("Streaming", "CUSTOM", 100, "2026-12-31"));

        mockMvc.perform(delete("/api/goals/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        // Preserved but flagged inactive.
        mockMvc.perform(get("/api/goals/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goal.active").value(false));

        // Hidden from the active-only view.
        mockMvc.perform(get("/api/goals?active=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void userCannotAccessAnotherUsersGoal() throws Exception {
        String ownerToken = register(email(), password).get("accessToken");
        String ownerGoalId = createGoal(ownerToken, goalBody("Private", "SAVINGS", 1000, "2026-12-31"));

        String otherToken = register(email(), password).get("accessToken");
        mockMvc.perform(get("/api/goals/" + ownerGoalId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void summaryRollsUpAcrossGoals() throws Exception {
        String token = register(email(), password).get("accessToken");
        createGoal(token, goalBody("A", "SAVINGS", 1000, "2026-12-31"));
        createGoal(token, goalBody("B", "DEBT_PAYOFF", 2000, "2026-12-31"));

        mockMvc.perform(get("/api/goals/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalGoals").value(2))
                .andExpect(jsonPath("$.data.activeGoals").value(2))
                .andExpect(jsonPath("$.data.totalTarget").value(3000))
                .andExpect(jsonPath("$.data.goals.length()").value(2));
    }

    private Map<String, Object> goalBody(String name, String type, Number target, String targetDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", type);
        body.put("targetAmount", target);
        body.put("targetDate", targetDate);
        return body;
    }
}
