package com.nova.ai.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nova.ai.CopilotService;
import com.nova.ai.IntentType;
import com.nova.ai.dto.ChatRequest;
import com.nova.ai.dto.ChatResponse;
import com.nova.auth.security.NovaUserPrincipal;
import com.nova.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class AiCopilotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CopilotService copilotService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        User user = new User("user@example.com", "Test User", "hash", com.nova.user.Role.USER,
                com.nova.user.AccountStatus.ACTIVE, "USD");
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, userId);
        NovaUserPrincipal principal = new NovaUserPrincipal(user);
        SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void chatReturnsOkEnvelope() throws Exception {
        ChatResponse response = new ChatResponse(
                UUID.randomUUID(), "You spent the most on Food.", IntentType.SPENDING, null,
                List.of("Compare with last month"));
        when(copilotService.chat(any(), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/copilot/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"Where did I spend the most?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("You spent the most on Food."))
                .andExpect(jsonPath("$.data.intent").value("SPENDING"));
    }

    @Test
    void chatRejectsEmptyMessage() throws Exception {
        mockMvc.perform(post("/api/copilot/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void suggestionsReturnsList() throws Exception {
        when(copilotService.suggestedQuestions()).thenReturn(List.of("How much did I save?", "Show my receipts"));
        mockMvc.perform(get("/api/copilot/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").exists());
    }

    @Test
    void resetReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/copilot/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
