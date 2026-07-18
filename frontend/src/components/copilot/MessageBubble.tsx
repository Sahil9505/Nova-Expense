import { Check, Copy, Sparkles } from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/lib/utils';
import type { ChatMessage, DataReference } from '@/types';
import { Badge } from '@/components/ui/badge';
import { CopilotMarkdown } from './CopilotMarkdown';

interface MessageBubbleProps {
  message: ChatMessage;
  dataReference?: DataReference | null;
  suggestions?: string[];
  onSuggestionClick?: (question: string) => void;
}

/** Renders one chat turn: the message, optional data card, and follow-up chips. */
export function MessageBubble({ message, dataReference, suggestions, onSuggestionClick }: MessageBubbleProps) {
  const [copied, setCopied] = useState(false);
  const isUser = message.role === 'user';

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      /* clipboard may be unavailable; ignore */
    }
  };

  if (isUser) {
    return (
      <div className="flex justify-end" aria-label="Your message">
        <div className="max-w-[85%] rounded-2xl rounded-br-sm bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-glow-primary">
          {message.content}
        </div>
      </div>
    );
  }

  return (
    <div className="flex gap-3" aria-label="Nova's response">
      <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/15 text-primary ring-1 ring-primary/25">
        <Sparkles className="h-4 w-4" aria-hidden="true" />
      </div>
      <div className="min-w-0 flex-1">
        <div
          className={cn(
            'rounded-2xl rounded-tl-sm border border-border bg-surface/80 p-3.5',
            message.error && 'border-warning/40 bg-warning/10',
          )}
        >
          {message.pending ? (
            <TypingDots />
          ) : (
            <CopilotMarkdown content={message.content} />
          )}
        </div>

        {!message.pending && !message.error && (
          <button
            type="button"
            onClick={copy}
            className="mt-1.5 inline-flex items-center gap-1 text-xs text-muted-foreground transition-colors hover:text-foreground"
          >
            {copied ? <Check className="h-3.5 w-3.5 text-success" /> : <Copy className="h-3.5 w-3.5" />}
            {copied ? 'Copied' : 'Copy'}
          </button>
        )}

        {dataReference && <DataReferenceCard reference={dataReference} />}

        {suggestions && suggestions.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-2">
            {suggestions.map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => onSuggestionClick?.(s)}
                className="rounded-full border border-border bg-surface/60 px-3 py-1.5 text-xs font-medium text-muted-foreground transition-all hover:border-primary/40 hover:bg-surface-2/70 hover:text-foreground"
              >
                {s}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/** Compact, read-only summary of the figures the assistant based its answer on. */
function DataReferenceCard({ reference }: { reference: DataReference }) {
  return (
    <div className="mt-3 rounded-xl border border-border bg-surface-2/50 p-3">
      <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        {reference.title}
      </p>
      {reference.facts.length > 0 && (
        <div className="grid grid-cols-2 gap-x-4 gap-y-1.5">
          {reference.facts.map((f) => (
            <div key={f.label} className="flex items-baseline justify-between gap-2">
              <span className="text-xs text-muted-foreground">{f.label}</span>
              <span className="text-sm font-semibold tabular-nums text-foreground">{f.value}</span>
            </div>
          ))}
        </div>
      )}
      {reference.items.length > 0 && (
        <ul className="mt-2 space-y-1.5 border-t border-border/70 pt-2">
          {reference.items.map((item) => (
            <li key={item.label} className="flex items-baseline justify-between gap-2 text-xs">
              <span className="truncate text-muted-foreground">{item.label}</span>
              <span className="flex items-center gap-1.5">
                <span className="font-medium tabular-nums text-foreground">{item.value}</span>
                {item.hint && (
                  <Badge variant="outline" className="px-1.5 py-0 text-[10px]">
                    {item.hint}
                  </Badge>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** Three pulsing dots shown while the assistant is "thinking". */
function TypingDots() {
  return (
    <div className="flex items-center gap-1 py-1" aria-label="Nova is typing">
      {[0, 150, 300].map((delay) => (
        <span
          key={delay}
          className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground/60"
          style={{ animationDelay: `${delay}ms` }}
        />
      ))}
    </div>
  );
}
