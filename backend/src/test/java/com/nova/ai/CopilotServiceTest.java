package com.nova.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nova.ai.dto.AiMessage;
import com.nova.ai.dto.ChatRequest;
import com.nova.ai.dto.ChatResponse;
import com.nova.ai.port.AiChatGateway;
import com.nova.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

class CopilotServiceTest {

    private IntentResolver intentResolver;
    private PeriodResolver periodResolver;
    private FinancialContextBuilder contextBuilder;
    private PromptBuilder promptBuilder;
    private AiChatGateway chatGateway;
    private ConversationService conversationService;
    private AiResponseMapper responseMapper;
    private com.nova.ai.config.AiProperties properties;
    private CopilotService service;

    private final UUID userId = UUID.randomUUID();
    private final PeriodResolver.Window window = new PeriodResolver.Window(
            java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now().plusMonths(1));

    @BeforeEach
    void setUp() {
        intentResolver = mock(IntentResolver.class);
        periodResolver = mock(PeriodResolver.class);
        contextBuilder = mock(FinancialContextBuilder.class);
        promptBuilder = mock(PromptBuilder.class);
        chatGateway = mock(AiChatGateway.class);
        conversationService = mock(ConversationService.class);
        responseMapper = mock(AiResponseMapper.class);
        properties = mock(com.nova.ai.config.AiProperties.class);

        ConversationService.Conversation conversation = new ConversationService.Conversation(
                UUID.randomUUID(), userId, new java.util.ArrayList<>(), Instant.now());
        lenient().when(conversationService.getOrCreate(eq(userId), any())).thenReturn(conversation);
        lenient().when(conversationService.list(eq(userId))).thenReturn(List.of());

        when(intentResolver.resolve(anyString(), any())).thenReturn(IntentType.SPENDING);
        when(periodResolver.currentMonth()).thenReturn(window);
        when(contextBuilder.build(eq(userId), eq(IntentType.SPENDING), eq(window)))
                .thenReturn(new FinancialContext("USD", null, null, null, null, null, null, null, null, null));
        when(promptBuilder.systemInstruction()).thenReturn("SYSTEM");
        when(promptBuilder.buildContextDocument(any(), any())).thenReturn("CONTEXT");
        when(chatGateway.isAvailable()).thenReturn(true);
        when(chatGateway.generate(anyString(), any())).thenReturn("You spent the most on Food.");
        when(responseMapper.toReference(any(), eq(IntentType.SPENDING))).thenReturn(null);
        when(responseMapper.buildResponse(any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new ChatResponse(
                        inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                        inv.getArgument(3), inv.getArgument(4)));

        service = new CopilotService(intentResolver, periodResolver, contextBuilder, promptBuilder,
                chatGateway, conversationService, responseMapper, properties);
    }

    @Test
    void chatReturnsGroundedResponseAndPersistsTurns() {
        ChatRequest request = new ChatRequest("Where did I spend the most?", null);
        ChatResponse response = service.chat(userId, request);

        assertNotNull(response.conversationId());
        assertEquals("You spent the most on Food.", response.answer());
        assertEquals(IntentType.SPENDING, response.intent());
        verify(chatGateway).generate(eq("SYSTEM"), any());
        // User turn + assistant turn must both be persisted.
        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(conversationService, org.mockito.Mockito.atLeast(2))
                .append(eq(userId), any(), captor.capture());
        List<AiMessage> saved = captor.getAllValues();
        assertTrue(saved.stream().anyMatch(m -> "user".equals(m.role())));
        assertTrue(saved.stream().anyMatch(m -> "assistant".equals(m.role())));
    }

    @Test
    void gatewayFailureDegradesToFriendlyFallback() {
        when(chatGateway.generate(anyString(), any()))
                .thenThrow(new com.nova.ai.AiCopilotException(ErrorCode.AI_UNAVAILABLE, "unreachable"));
        when(properties.getFallbackMessage()).thenReturn("AI is down.");

        ChatRequest request = new ChatRequest("Where did I spend the most?", null);
        ChatResponse response = service.chat(userId, request);

        assertEquals("AI is down.", response.answer());
        assertEquals(IntentType.SPENDING, response.intent());
    }

    @Test
    void suggestedQuestionsAreReturned() {
        assertTrue(service.suggestedQuestions().size() >= 8);
    }
}
