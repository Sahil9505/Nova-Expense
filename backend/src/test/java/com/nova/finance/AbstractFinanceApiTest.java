package com.nova.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.finance.budget.BudgetRepository;
import com.nova.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared harness for finance API tests: boots the full Spring context against the
 * in-memory H2 database, and provides helpers for registering users, minting
 * tokens, and creating the finance entities used by the scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractFinanceApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BudgetRepository budgetRepository;

    @BeforeEach
    void cleanBudgets() {
        // The in-memory test database is shared across test methods (ddl-auto=create-drop
        // only drops at the end of the run) and register/create commit their own
        // transactions. Budgets are user-scoped, so clearing them keeps Budget tests
        // isolated without affecting other finance suites.
        budgetRepository.deleteAll();
        budgetRepository.flush();
    }

    protected String email() {
        return "finance-" + UUID.randomUUID().toString().substring(0, 8) + "@nova.test";
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    /** Returns the {@code data} node of an ApiResponse envelope. */
    protected JsonNode parse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    protected Map<String, String> register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = parse(result);
        return Map.of(
                "accessToken", data.get("accessToken").asText(),
                "refreshToken", data.get("refreshToken").asText());
    }

    protected String accessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("accessToken").asText();
    }

    protected String createAccount(String token, String name, String type, String currency, Number balance)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", type);
        body.put("currency", currency);
        body.put("balance", balance);
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asText();
    }

    protected String createCategory(String token, String name, String type) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", type);
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asText();
    }

    protected String createTransaction(String token, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asText();
    }

    protected String createBudget(String token, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asText();
    }
}
