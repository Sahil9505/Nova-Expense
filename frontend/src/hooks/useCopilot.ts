import { useMutation, useQuery } from '@tanstack/react-query';
import { copilotApi } from '@/lib/api';
import type { CopilotChatRequest, CopilotChatResponse } from '@/types';

/** Sends a question to the copilot. On success the caller persists the answer. */
export function useCopilotChat() {
  return useMutation<CopilotChatResponse, Error, CopilotChatRequest>({
    mutationFn: (payload) => copilotApi.chat(payload),
  });
}

/** Example questions the UI surfaces as suggestion chips. */
export function useCopilotSuggestions() {
  return useQuery<string[]>({
    queryKey: ['copilot', 'suggestions'],
    queryFn: () => copilotApi.suggestions(),
    staleTime: Infinity,
  });
}

/** Clears copilot conversation history (single thread or all). */
export function useCopilotReset() {
  return useMutation<void, Error, string | undefined>({
    mutationFn: (conversationId) => copilotApi.reset(conversationId),
  });
}
