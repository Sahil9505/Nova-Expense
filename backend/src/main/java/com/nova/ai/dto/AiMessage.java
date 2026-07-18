package com.nova.ai.dto;

import com.nova.ai.IntentType;
import java.time.Instant;
import java.util.UUID;

/**
 * A single turn in a copilot conversation. Persisted only in the lightweight,
 * per-user, in-memory conversation store ({@code ConversationService}). The AI
 * never owns financial data — these messages carry the user's question, the
 * assistant's explanation, and lightweight metadata used to keep follow-up
 * questions grounded.
 */
public record AiMessage(
        UUID id,
        String role,
        String content,
        IntentType intent,
        Instant timestamp
) {
    public static AiMessage user(UUID id, String content, Instant timestamp) {
        return new AiMessage(id, "user", content, null, timestamp);
    }

    public static AiMessage assistant(UUID id, String content, IntentType intent, Instant timestamp) {
        return new AiMessage(id, "assistant", content, intent, timestamp);
    }
}
