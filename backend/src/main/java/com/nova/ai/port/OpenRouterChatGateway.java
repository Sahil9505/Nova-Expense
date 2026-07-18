package com.nova.ai.port;

import com.nova.ai.AiCopilotException;
import com.nova.ai.config.AiProperties;
import com.nova.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import java.util.List;

/**
 * OpenRouter implementation of {@link AiChatGateway}, built on Spring AI's
 * provider-agnostic {@link ChatClient}. OpenRouter speaks the OpenAI-compatible
 * chat-completions protocol, so this adapter uses the OpenAI {@link ChatClient} pointed
 * at OpenRouter's base URL (see {@code OpenRouterConfig}). It only supplies Nova's
 * defaults (model, temperature, token ceiling, request timeout) and translates
 * provider failures into the domain's {@link AiCopilotException}.
 *
 * <p>Not annotated with {@code @Component}: the single instance is created by
 * {@code OpenRouterConfig#aiChatGateway} (only when a provider key is configured), so
 * the classpath scan never instantiates a second, unconfigured copy.</p>
 *
 * <p>Because the AI domain depends solely on {@code AiChatGateway}, replacing OpenRouter
 * later means adding another implementation of that interface — no change to intent
 * resolution, context building, prompt construction, or response mapping.</p>
 */
public class OpenRouterChatGateway implements AiChatGateway {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterChatGateway.class);

    private final ChatClient chatClient;
    private final boolean available;

    public OpenRouterChatGateway(ChatClient chatClient, AiProperties properties) {
        // The client is only assembled when a key exists, so a null client means
        // "unavailable" and every generate() call degrades to a friendly error.
        this.chatClient = chatClient;
        this.available = chatClient != null && properties.isConfigured();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String generate(String systemInstruction, List<String> conversation) {
        if (!available) {
            throw new AiCopilotException(ErrorCode.AI_UNAVAILABLE,
                    "The AI assistant is not configured on this server.");
        }
        if (conversation == null || conversation.isEmpty()) {
            throw new AiCopilotException(ErrorCode.AI_INVALID_RESPONSE,
                    "Nothing to send to the assistant.");
        }

        StringBuilder userTurn = new StringBuilder();
        for (int i = 0; i < conversation.size(); i++) {
            userTurn.append(i % 2 == 0 ? "User: " : "Assistant: ")
                    .append(conversation.get(i))
                    .append('\n');
        }

        ChatClientRequestSpec spec = chatClient.prompt()
                .system(systemInstruction)
                .user(userTurn.toString());

        try {
            String content = spec.call().content();
            if (content == null || content.isBlank()) {
                throw new AiCopilotException(ErrorCode.AI_INVALID_RESPONSE,
                        "The assistant returned an empty response.");
            }
            return content.trim();
        } catch (AiCopilotException ex) {
            throw ex;
        } catch (Exception ex) {
            throw translate(ex, systemInstruction);
        }
    }

    private AiCopilotException translate(Exception ex, String systemInstruction) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (message.contains("timeout") || message.contains("deadline") || message.contains("timed out")) {
            return new AiCopilotException(ErrorCode.AI_TIMEOUT, "The assistant took too long to respond.", ex);
        }
        if (message.contains("rate") || message.contains("429") || message.contains("quota") || message.contains("resource exhausted")) {
            return new AiCopilotException(ErrorCode.AI_RATE_LIMITED, "The assistant is busy right now.", ex);
        }
        if (message.contains("api key") || message.contains("permission") || message.contains("401") || message.contains("403")) {
            return new AiCopilotException(ErrorCode.AI_UNAVAILABLE, "The assistant is not configured correctly.", ex);
        }
        if (message.contains("unavailable") || message.contains("503") || message.contains("overloaded")) {
            return new AiCopilotException(ErrorCode.AI_UNAVAILABLE, "The assistant is temporarily unavailable.", ex);
        }
        log.warn("OpenRouter request failed: {}", ex.toString());
        return new AiCopilotException(ErrorCode.AI_UNAVAILABLE, "The assistant could not be reached.", ex);
    }
}
