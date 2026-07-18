package com.nova.ai;

import com.nova.ai.dto.AiMessage;
import com.nova.ai.dto.ConversationSummary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight, per-user conversation store for the copilot. Conversations are kept
 * in memory only (bounded per user) so follow-up questions stay grounded without a
 * database migration; this matches the "lightweight conversation history" scope of
 * the phase. History is scoped by the authenticated user id — one user can never
 * read another's thread.
 *
 * <p>If durable history is required later, swap this implementation for one backed
 * by a repository; the rest of the AI domain depends only on these methods.</p>
 */
@Service
public class ConversationService {

    private static final int MAX_MESSAGES_PER_THREAD = 24;
    private static final int MAX_THREADS_PER_USER = 20;

    private final Map<UUID, Map<UUID, Conversation>> store = new ConcurrentHashMap<>();

    /** Get an existing thread or create a new one for the user. */
    public Conversation getOrCreate(UUID userId, String conversationId) {
        Map<UUID, Conversation> userThreads = store.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        UUID parsed = parse(conversationId);
        if (parsed != null) {
            Conversation existing = userThreads.get(parsed);
            if (existing != null) {
                return existing;
            }
        }
        UUID id = UUID.randomUUID();
        Conversation conversation = new Conversation(id, userId, new ArrayList<>(), Instant.now());
        userThreads.put(id, conversation);
        evictIfNeeded(userId, userThreads);
        return conversation;
    }

    public Conversation append(UUID userId, String conversationId, AiMessage message) {
        Conversation conversation = getOrCreate(userId, conversationId);
        conversation.messages().add(message);
        if (conversation.messages().size() > MAX_MESSAGES_PER_THREAD) {
            conversation.messages().subList(0, conversation.messages().size() - MAX_MESSAGES_PER_THREAD).clear();
        }
        conversation.setUpdatedAt(Instant.now());
        return conversation;
    }

    public List<AiMessage> history(UUID userId, UUID conversationId) {
        Conversation conversation = getOrCreate(userId, conversationId.toString());
        return List.copyOf(conversation.messages());
    }

    public List<ConversationSummary> list(UUID userId) {
        Map<UUID, Conversation> userThreads = store.get(userId);
        if (userThreads == null) {
            return List.of();
        }
        return userThreads.values().stream()
                .map(c -> new ConversationSummary(
                        c.id(),
                        titleOf(c),
                        c.messages().size(),
                        c.updatedAt()))
                .sorted(Comparator.comparing(ConversationSummary::updatedAt).reversed())
                .toList();
    }

    /**
     * Clear the user's history. With a valid {@code conversationId} clears that one
     * thread; when omitted (null/blank) clears every thread. A malformed id is a
     * no-op — it must never fall through to clearing everything.
     */
    public void reset(UUID userId, String conversationId) {
        Map<UUID, Conversation> userThreads = store.get(userId);
        if (userThreads == null) {
            return;
        }
        if (conversationId == null || conversationId.isBlank()) {
            userThreads.clear();
            return;
        }
        UUID parsed = parse(conversationId);
        if (parsed != null) {
            userThreads.remove(parsed);
        }
    }

    private void evictIfNeeded(UUID userId, Map<UUID, Conversation> userThreads) {
        if (userThreads.size() <= MAX_THREADS_PER_USER) {
            return;
        }
        userThreads.values().stream()
                .min(Comparator.comparing(Conversation::updatedAt))
                .ifPresent(oldest -> userThreads.remove(oldest.id()));
    }

    private String titleOf(Conversation c) {
        return c.messages().stream()
                .filter(m -> "user".equals(m.role()))
                .map(AiMessage::content)
                .findFirst()
                .orElse("New conversation");
    }

    private UUID parse(String id) {
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Mutable conversation handle. {@code messages} is an ArrayList so the service
     * can append, and {@code updatedAt} is refreshed on each append.
     */
    public static final class Conversation {
        private final UUID id;
        private final UUID userId;
        private final List<AiMessage> messages;
        private Instant updatedAt;

        public Conversation(UUID id, UUID userId, List<AiMessage> messages, Instant updatedAt) {
            this.id = id;
            this.userId = userId;
            this.messages = messages;
            this.updatedAt = updatedAt;
        }

        public UUID id() {
            return id;
        }

        public UUID userId() {
            return userId;
        }

        public List<AiMessage> messages() {
            return messages;
        }

        public Instant updatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
