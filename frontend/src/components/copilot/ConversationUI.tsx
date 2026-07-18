import { CornerDownLeft, Send, Sparkles } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useCopilotChat, useCopilotSuggestions } from '@/hooks/useCopilot';
import { useCopilotStore } from '@/store/copilotStore';
import type { ChatMessage, CopilotChatResponse } from '@/types';
import { MessageBubble } from './MessageBubble';

interface ConversationUIProps {
  /** 'panel' renders the floating-drawer variant; 'page' the full-screen route. */
  variant?: 'panel' | 'page';
}

const GREETING =
  "Hi, I'm Nova — your AI Financial Copilot. Ask me about your spending, budgets, goals, or receipts, and I'll explain what your own data shows.";

/** The chat surface. Reused by the floating panel and the /copilot page. */
export function ConversationUI({ variant = 'panel' }: ConversationUIProps) {
  const messages = useCopilotStore((s) => s.messages);
  const consumePendingQuestion = useCopilotStore((s) => s.consumePendingQuestion);
  const addMessage = useCopilotStore((s) => s.addMessage);
  const setMessages = useCopilotStore((s) => s.setMessages);
  const setConversationId = useCopilotStore((s) => s.setConversationId);

  const chat = useCopilotChat();
  const { data: suggestions = [] } = useCopilotSuggestions();
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to the newest turn.
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages]);

  // Send a question passed in when the panel/page opened (from a widget or link).
  useEffect(() => {
    const q = consumePendingQuestion();
    if (q) void send(q);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function applyResponse(resp: CopilotChatResponse) {
    setConversationId(resp.conversationId);
    addMessage({
      id: `a-${resp.conversationId}-${Date.now()}`,
      role: 'assistant',
      content: resp.answer,
      intent: resp.intent,
      dataReference: resp.dataReference,
      suggestions: resp.suggestions,
      timestamp: new Date().toISOString(),
    });
  }

  function applyError(message: string) {
    addMessage({
      id: `e-${Date.now()}`,
      role: 'assistant',
      content: message,
      intent: null,
      error: true,
      timestamp: new Date().toISOString(),
    });
  }

  async function send(text: string) {
    const question = text.trim();
    if (!question || isSending) return;
    setIsSending(true);

    const userMessage: ChatMessage = {
      id: `u-${Date.now()}`,
      role: 'user',
      content: question,
      timestamp: new Date().toISOString(),
    };
    const pendingAssistant: ChatMessage = {
      id: `pending-${Date.now()}`,
      role: 'assistant',
      content: '',
      pending: true,
    };
    setMessages([...useCopilotStore.getState().messages, userMessage, pendingAssistant]);
    setInput('');

    try {
      const resp = await chat.mutateAsync({
        message: question,
        conversationId: useCopilotStore.getState().conversationId,
      });
      // Replace the pending bubble with the real answer.
      const current = useCopilotStore.getState().messages.filter((m) => !m.pending);
      setMessages([...current]);
      applyResponse(resp);
    } catch (err) {
      const current = useCopilotStore.getState().messages.filter((m) => !m.pending);
      setMessages([...current]);
      const message =
        err instanceof Error && err.message
          ? err.message
          : 'I could not reach the assistant. Please try again.';
      applyError(message);
    } finally {
      setIsSending(false);
    }
  }

  function onSuggestionClick(question: string) {
    void send(question);
  }

  const lastAssistant = [...messages].reverse().find((m) => m.role === 'assistant' && !m.pending);
  const lastAssistantDataRef = lastAssistant?.dataReference;
  const lastAssistantSuggestions = lastAssistant?.suggestions;
  const isEmpty = messages.length === 0;

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div ref={scrollRef} className="min-h-0 flex-1 space-y-4 overflow-y-auto px-4 py-4">
        {isEmpty ? (
          <EmptyState
            greeting={GREETING}
            suggestions={suggestions}
            onSuggestionClick={onSuggestionClick}
          />
        ) : (
          messages.map((m) => (
            <MessageBubble
              key={m.id}
              message={m}
              dataReference={m.role === 'assistant' && m.id === lastAssistant?.id ? lastAssistantDataRef : undefined}
              suggestions={m.role === 'assistant' && m.id === lastAssistant?.id ? lastAssistantSuggestions : undefined}
              onSuggestionClick={onSuggestionClick}
            />
          ))
        )}
      </div>

      <Composer
        value={input}
        onChange={setInput}
        onSend={() => void send(input)}
        disabled={isSending}
        variant={variant}
      />
    </div>
  );
}

/** Welcome screen with suggestion chips shown before the first message. */
function EmptyState({
  greeting,
  suggestions,
  onSuggestionClick,
}: {
  greeting: string;
  suggestions: string[];
  onSuggestionClick: (q: string) => void;
}) {
  const starter = suggestions.slice(0, 6);
  return (
    <div className="flex h-full flex-col items-center justify-center text-center">
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/15 text-primary ring-1 ring-primary/25">
        <Sparkles className="h-6 w-6" aria-hidden="true" />
      </div>
      <p className="max-w-md text-sm text-muted-foreground">{greeting}</p>
      {starter.length > 0 && (
        <div className="mt-5 flex max-w-md flex-wrap justify-center gap-2">
          {starter.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => onSuggestionClick(s)}
              className="rounded-full border border-border bg-surface/60 px-3 py-1.5 text-xs font-medium text-muted-foreground transition-all hover:border-primary/40 hover:bg-surface-2/70 hover:text-foreground"
            >
              {s}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

/** Message input with send button and Enter-to-send. */
function Composer({
  value,
  onChange,
  onSend,
  disabled,
  variant,
}: {
  value: string;
  onChange: (v: string) => void;
  onSend: () => void;
  disabled: boolean;
  variant?: 'panel' | 'page';
}) {
  return (
    <div className={cn('border-t border-border bg-surface/60 p-3', variant === 'page' && 'p-4')}>
      <div className="flex items-end gap-2">
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              onSend();
            }
          }}
          rows={1}
          placeholder="Ask Nova about your finances…"
          className="max-h-32 min-h-[42px] flex-1 resize-none rounded-xl border border-border bg-background/60 px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/30"
        />
        <Button
          type="button"
          size="icon"
          onClick={onSend}
          disabled={disabled || value.trim().length === 0}
          aria-label="Send message"
          className="h-[42px] w-[42px] shrink-0 rounded-xl"
        >
          {disabled ? (
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground/40 border-t-primary-foreground" />
          ) : (
            <Send className="h-4 w-4" />
          )}
        </Button>
      </div>
      <p className="mt-1.5 flex items-center gap-1 px-1 text-[11px] text-muted-foreground">
        <CornerDownLeft className="h-3 w-3" /> Enter to send · Nova answers only from your data
      </p>
    </div>
  );
}
