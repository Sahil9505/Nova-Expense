package com.nova.ai.dto;

import com.nova.ai.IntentType;
import java.util.List;
import java.util.UUID;

/**
 * The copilot's answer to a question. {@code answer} is markdown (the UI renders
 * it). {@code intent} records how the question was classified (useful for
 * analytics and for grounding follow-ups). {@code dataReference} is an optional
 * structured summary of the figures used. {@code suggestions} are follow-up
 * prompts the UI can surface as chips. {@code conversationId} ties the turn to its
 * thread so the client can keep talking.
 */
public record ChatResponse(
        UUID conversationId,
        String answer,
        IntentType intent,
        DataReference dataReference,
        List<String> suggestions
) {
}
