package com.nova.ai.web;

import com.nova.ai.CopilotService;
import com.nova.ai.dto.ChatRequest;
import com.nova.ai.dto.ChatResponse;
import com.nova.ai.dto.ConversationSummary;
import com.nova.auth.security.NovaUserPrincipal;
import com.nova.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST surface for the AI Financial Copilot. Every endpoint is protected and
 * operates strictly in the authenticated user's scope — the principal's id is the
 * only ownership boundary used when gathering data, so one user can never see
 * another's figures.
 */
@RestController
@RequestMapping("/api/copilot")
@SecurityRequirement(name = "bearerAuth")
public class AiCopilotController {

    private final CopilotService copilotService;

    public AiCopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    /**
     * Ask the copilot a question. Optionally continues an existing conversation via
     * {@code conversationId} so follow-up questions stay grounded.
     */
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = copilotService.chat(principal.getUserId(), request);
        return ApiResponse.ok("Here's what I found.", response);
    }

    /** List the user's copilot conversations (metadata only). */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationSummary>> conversations(
            @AuthenticationPrincipal NovaUserPrincipal principal) {
        return ApiResponse.ok(copilotService.history(principal.getUserId()));
    }

    /** Example questions the UI can offer as chips. */
    @GetMapping("/suggestions")
    public ApiResponse<List<String>> suggestions() {
        return ApiResponse.ok(copilotService.suggestedQuestions());
    }

    /**
     * Reset conversation history. With {@code conversationId} clears one thread;
     * without it, clears every thread for the user.
     */
    @DeleteMapping("/conversations")
    public ApiResponse<Void> reset(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam(required = false) String conversationId) {
        copilotService.reset(principal.getUserId(), conversationId);
        return ApiResponse.ok("Conversation cleared.");
    }
}
