package com.nova.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nova.ai.dto.AiMessage;
import com.nova.ai.dto.ConversationSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Exercises the lightweight, per-user, in-memory conversation store: thread
 * creation, message append, bounding, eviction, per-user isolation, and the two
 * reset modes (single thread vs. all threads).
 */
class ConversationServiceTest {

    private ConversationService service;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ConversationService();
    }

    @Test
    void getOrCreateReturnsNewAndExistingThread() {
        ConversationService.Conversation created = service.getOrCreate(alice, null);
        assertNotNull(created.id());
        assertEquals(alice, created.userId());

        ConversationService.Conversation same = service.getOrCreate(alice, created.id().toString());
        assertEquals(created.id(), same.id());
    }

    @Test
    void appendPersistsUserAndAssistantTurns() {
        ConversationService.Conversation c = service.getOrCreate(alice, null);
        service.append(alice, c.id().toString(), AiMessage.user(UUID.randomUUID(), "Hi", Instant.now()));
        service.append(alice, c.id().toString(),
                AiMessage.assistant(UUID.randomUUID(), "Hello", IntentType.SPENDING, Instant.now()));

        ConversationService.Conversation reloaded = service.getOrCreate(alice, c.id().toString());
        assertEquals(2, reloaded.messages().size());
        assertEquals("user", reloaded.messages().get(0).role());
        assertEquals("assistant", reloaded.messages().get(1).role());
    }

    @Test
    void threadsAreIsolatedPerUser() {
        ConversationService.Conversation aliceThread = service.getOrCreate(alice, null);
        service.append(alice, aliceThread.id().toString(), AiMessage.user(UUID.randomUUID(), "alice secret", Instant.now()));

        ConversationService.Conversation bobThread = service.getOrCreate(bob, null);
        service.append(bob, bobThread.id().toString(), AiMessage.user(UUID.randomUUID(), "bob secret", Instant.now()));

        List<ConversationSummary> aliceSummaries = service.list(alice);
        assertEquals(1, aliceSummaries.size());
        assertEquals("alice secret", aliceSummaries.get(0).title());

        List<ConversationSummary> bobSummaries = service.list(bob);
        assertEquals(1, bobSummaries.size());
        assertEquals("bob secret", bobSummaries.get(0).title());
    }

    @Test
    void resetSingleThreadKeepsOthers() {
        ConversationService.Conversation first = service.getOrCreate(alice, null);
        ConversationService.Conversation second = service.getOrCreate(alice, null);
        assertFalse(first.id().equals(second.id()));

        service.reset(alice, first.id().toString());
        assertEquals(1, service.list(alice).size());
        assertNotNull(service.getOrCreate(alice, second.id().toString()));
    }

    @Test
    void resetAllClearsEveryThread() {
        service.getOrCreate(alice, null);
        service.getOrCreate(alice, null);
        service.getOrCreate(bob, null);

        service.reset(alice, null);
        assertTrue(service.list(alice).isEmpty());
        // Bob's data is untouched by Alice's reset.
        assertEquals(1, service.list(bob).size());
    }

    @Test
    void unknownConversationIdCreatesNewThread() {
        assertNotNull(service.getOrCreate(alice, "not-a-uuid"));
    }

    @Test
    void historyReturnsImmutableSnapshot() {
        ConversationService.Conversation c = service.getOrCreate(alice, null);
        service.append(alice, c.id().toString(), AiMessage.user(UUID.randomUUID(), "q", Instant.now()));

        List<AiMessage> history = service.history(alice, c.id());
        assertEquals(1, history.size());
        // The returned list is an immutable copy — callers cannot mutate the store.
        assertThrows(UnsupportedOperationException.class, history::clear);
        assertEquals(1, service.history(alice, c.id()).size());
    }
}
