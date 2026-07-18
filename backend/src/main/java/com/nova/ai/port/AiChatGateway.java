package com.nova.ai.port;

import java.util.List;

/**
 * Provider-agnostic contract the copilot uses to generate text from a language
 * model. The rest of the AI domain depends only on this interface, never on
 * Spring AI, OpenRouter, or any SDK — so the provider can be swapped (Vertex AI, a
 * self-hosted model, a future Anthropic integration) by adding a new
 * implementation behind the same contract.
 */
public interface AiChatGateway {

    /**
     * Generate a reply from a system instruction and an ordered list of
     * user/assistant turns (oldest first). Implementations must raise
     * {@link com.nova.ai.AiCopilotException} with an appropriate {@code ErrorCode}
     * on timeout, rate limit, network failure, or invalid response — never a raw
     * provider exception.
     *
     * @param systemInstruction the fixed persona + grounding rules
     * @param conversation       alternating turns; element 0 is a user message
     * @return the model's reply text
     */
    String generate(String systemInstruction, List<String> conversation);

    /**
     * @return true if a model is configured and reachable for a call; used to
     *         short-circuit with a friendly message instead of a failed request.
     */
    boolean isAvailable();
}
