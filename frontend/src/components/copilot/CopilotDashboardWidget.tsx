import { ArrowUpRight, Sparkles } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { useCopilotStore } from '@/store/copilotStore';

const PROMPTS = [
  'Where did I spend the most this month?',
  'How healthy are my finances?',
  'Which goal is closest to completion?',
];

/**
 * Compact entry point on the dashboard. Opens the copilot with a starter question
 * so users can go from insight to answer in one tap. The dashboard layout is
 * extended, not redesigned.
 */
export function CopilotDashboardWidget() {
  const open = useCopilotStore((s) => s.open);

  return (
    <Card className="flex flex-col gap-3 p-5">
      <div className="flex items-center gap-2.5">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/15 text-primary ring-1 ring-primary/25">
          <Sparkles className="h-4 w-4" aria-hidden="true" />
        </div>
        <div>
          <p className="text-sm font-semibold leading-tight">Ask Nova AI</p>
          <p className="text-xs text-muted-foreground">Your financial copilot</p>
        </div>
      </div>

      <p className="text-sm text-muted-foreground">
        Ask plain-language questions about your spending, budgets, goals, and receipts.
      </p>

      <div className="flex flex-col gap-1.5">
        {PROMPTS.map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => open(p)}
            className="group flex items-center justify-between rounded-lg border border-border bg-surface/60 px-3 py-2 text-left text-sm text-foreground transition-all hover:border-primary/40 hover:bg-surface-2/70"
          >
            <span className="truncate">{p}</span>
            <ArrowUpRight className="h-4 w-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-primary" />
          </button>
        ))}
      </div>

      <button
        type="button"
        onClick={() => open()}
        className="mt-1 inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground shadow-glow-primary transition-all hover:-translate-y-px hover:bg-primary/90"
      >
        <Sparkles className="h-4 w-4" /> Open copilot
      </button>
    </Card>
  );
}
