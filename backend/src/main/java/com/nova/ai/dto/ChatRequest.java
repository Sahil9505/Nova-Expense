package com.nova.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming chat request from the copilot UI.
 *
 * <p>{@code message} is the user's natural-language question. {@code conversationId}
 * is optional: when supplied the assistant continues an existing thread (so
 * follow-up questions like "what about last month?" stay grounded); when omitted a
 * new thread is started. The id is opaque to the client and scoped to the
 * authenticated user on the server.</p>
 */
public record ChatRequest(

        @NotBlank(message = "Ask a question to talk to Nova.")
        @Size(max = 1000, message = "Your question is too long.")
        String message,

        String conversationId
) {
}
