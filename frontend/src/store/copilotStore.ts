import { create } from 'zustand';
import type { ChatMessage } from '@/types';

interface CopilotState {
  /** Whether the floating chat panel is open. */
  isOpen: boolean;
  /** Messages for the active conversation (kept in the panel across navigation). */
  messages: ChatMessage[];
  /** Server-side conversation id, enabling grounded follow-ups. */
  conversationId: string | null;
  /** A question to send as soon as the panel opens (from a widget/deeplink). */
  pendingQuestion: string | null;

  open: (question?: string) => void;
  close: () => void;
  toggle: () => void;
  setMessages: (messages: ChatMessage[]) => void;
  addMessage: (message: ChatMessage) => void;
  setConversationId: (id: string | null) => void;
  consumePendingQuestion: () => string | null;
  resetConversation: () => void;
}

/**
 * Holds the copilot panel's UI state. The conversation itself lives partly here
 * (for instant rendering) and partly on the server (for history). Keeping it in a
 * store means the floating assistant is available from every page without losing
 * the thread on navigation.
 */
export const useCopilotStore = create<CopilotState>((set, get) => ({
  isOpen: false,
  messages: [],
  conversationId: null,
  pendingQuestion: null,

  open: (question?: string) =>
    set((state) => ({
      isOpen: true,
      pendingQuestion: question ?? state.pendingQuestion,
    })),
  close: () => set({ isOpen: false }),
  toggle: () => set((state) => ({ isOpen: !state.isOpen })),
  setMessages: (messages) => set({ messages }),
  addMessage: (message) => set((state) => ({ messages: [...state.messages, message] })),
  setConversationId: (conversationId) => set({ conversationId }),
  consumePendingQuestion: () => {
    const q = get().pendingQuestion;
    if (q) set({ pendingQuestion: null });
    return q;
  },
  resetConversation: () => set({ messages: [], conversationId: null }),
}));
