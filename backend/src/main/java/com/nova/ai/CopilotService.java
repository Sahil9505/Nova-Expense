package com.nova.ai;

import com.nova.ai.dto.AiMessage;
import com.nova.ai.dto.ChatRequest;
import com.nova.ai.dto.ChatResponse;
import com.nova.ai.dto.DataReference;
import com.nova.ai.config.AiProperties;
import com.nova.ai.port.AiChatGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates one copilot turn:
 *
 * <pre>
 *   resolve intent → gather context (existing services) → build prompt →
 *   call model (via AiChatGateway) → map response → persist in conversation.
 * </pre>
 *
 * Business logic stays in the domains; this service only sequences the AI
 * pipeline and guarantees the model is grounded in Nova's own data. Failures from
 * the model degrade to a friendly, still-valid {@link ChatResponse} so the UI can
 * render an error state without losing the thread.
 */
@Service
public class CopilotService {

    private static final Logger log = LoggerFactory.getLogger(CopilotService.class);

    private final IntentResolver intentResolver;
    private final PeriodResolver periodResolver;
    private final FinancialContextBuilder contextBuilder;
    private final PromptBuilder promptBuilder;
    private final AiChatGateway chatGateway;
    private final ConversationService conversationService;
    private final AiResponseMapper responseMapper;
    private final AiProperties properties;

    public CopilotService(
            IntentResolver intentResolver,
            PeriodResolver periodResolver,
            FinancialContextBuilder contextBuilder,
            PromptBuilder promptBuilder,
            AiChatGateway chatGateway,
            ConversationService conversationService,
            AiResponseMapper responseMapper,
            AiProperties properties) {
        this.intentResolver = intentResolver;
        this.periodResolver = periodResolver;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.chatGateway = chatGateway;
        this.conversationService = conversationService;
        this.responseMapper = responseMapper;
        this.properties = properties;
    }

    public ChatResponse chat(UUID userId, ChatRequest request) {
        ConversationService.Conversation conversation =
                conversationService.getOrCreate(userId, request.conversationId());

        AiMessage userMessage = AiMessage.user(UUID.randomUUID(), request.message(), Instant.now());
        conversationService.append(userId, conversation.id().toString(), userMessage);

        IntentType previousIntent = lastAssistantIntent(conversation);
        IntentType intent = intentResolver.resolve(request.message(), previousIntent);

        PeriodResolver.Window period = periodResolver.currentMonth();
        FinancialContext context = contextBuilder.build(userId, intent, period);

        String system = promptBuilder.systemInstruction();
        String contextDoc = promptBuilder.buildContextDocument(context, intent);

        List<String> turns = new ArrayList<>();
        turns.add(contextDoc);
        for (AiMessage m : conversation.messages()) {
            if (m.role().equals("user")) {
                turns.add(m.content());
            } else if (m.role().equals("assistant") && m.content() != null) {
                turns.add(m.content());
            }
        }
        // Ensure the latest user message is present (it is, as the last appended).
        if (turns.isEmpty() || !turns.get(turns.size() - 1).equals(request.message())) {
            turns.add(request.message());
        }

        String answer;
        try {
            if (!chatGateway.isAvailable()) {
                throw new AiCopilotException(com.nova.common.exception.ErrorCode.AI_UNAVAILABLE,
                        "The AI assistant is not configured on this server.");
            }
            answer = chatGateway.generate(system, turns);
        } catch (Exception ex) {
            log.warn("Copilot generation failed for user {}: {}", userId, ex.toString());
            answer = fallbackAnswer(ex);
        }

        AiMessage assistantMessage = AiMessage.assistant(UUID.randomUUID(), answer, intent, Instant.now());
        conversationService.append(userId, conversation.id().toString(), assistantMessage);

        DataReference reference = responseMapper.toReference(context, intent);
        List<String> suggestions = suggestionsFor(intent);
        return responseMapper.buildResponse(conversation.id(), answer, intent, reference, suggestions);
    }

    public List<com.nova.ai.dto.ConversationSummary> history(UUID userId) {
        return conversationService.list(userId);
    }

    public void reset(UUID userId, String conversationId) {
        conversationService.reset(userId, conversationId);
    }

    public List<String> suggestedQuestions() {
        return List.of(
                "Where did I spend the most money this month?",
                "How much did I spend on food?",
                "Compare this month with last month.",
                "Which budgets are close to being exhausted?",
                "How healthy are my finances?",
                "Which goal is closest to completion?",
                "Show my recent receipts.",
                "How much money did I save this month?",
                "How much can I still spend safely?",
                "What are my biggest subscriptions?"
        );
    }

    private IntentType lastAssistantIntent(ConversationService.Conversation conversation) {
        for (int i = conversation.messages().size() - 1; i >= 0; i--) {
            AiMessage m = conversation.messages().get(i);
            if ("assistant".equals(m.role()) && m.intent() != null) {
                return m.intent();
            }
        }
        return null;
    }

    private List<String> suggestionsFor(IntentType intent) {
        return switch (intent) {
            case SPENDING -> List.of("Compare this month with last month.", "Which categories grew the most?");
            case CASH_FLOW -> List.of("How much can I still spend safely?", "How much did I save this month?");
            case COMPARISON -> List.of("Why did my spending change?", "Which categories drove the difference?");
            case BUDGET -> List.of("Which budget is most at risk?", "How much is left in my food budget?");
            case GOALS -> List.of("Which goal should I focus on next?", "How close is my nearest goal?");
            case RECEIPTS -> List.of("Show my recent receipts.", "Which receipts are not yet linked?");
            case FINANCIAL_HEALTH, GENERAL_SUMMARY ->
                    List.of("Where did I spend the most this month?", "Which budgets are at risk?");
        };
    }

    private String fallbackAnswer(Exception ex) {
        return properties != null && properties.getFallbackMessage() != null
                ? properties.getFallbackMessage()
                : "I can't respond right now. Please try again in a moment.";
    }
}
