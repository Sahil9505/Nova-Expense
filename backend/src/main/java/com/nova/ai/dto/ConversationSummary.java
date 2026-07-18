package com.nova.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A lightweight view of one conversation thread for the history list. The full
 * message bodies are not returned here; the UI requests a thread by id only when
 * the user opens it.
 */
public record ConversationSummary(
        UUID id,
        String title,
        int messageCount,
        Instant updatedAt
) {
}
