import { CheckCircle2, HelpCircle, ShieldAlert } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { ReceiptField } from '@/types';

/** Confidence at or below this threshold is surfaced as "please review". */
export const LOW_CONFIDENCE_THRESHOLD = 60;

type ConfidenceTone = 'high' | 'low' | 'missing';

/**
 * Maps a detected field to a confidence tone. A missing field (no value) is
 * "missing"; a present field is "low" until it clears the threshold.
 */
export function confidenceTone(field: ReceiptField | null | undefined): ConfidenceTone {
  if (!field || field.value === null || field.value === undefined || field.value === '') {
    return 'missing';
  }
  return (field.confidence ?? 0) > LOW_CONFIDENCE_THRESHOLD ? 'high' : 'low';
}

const toneStyles: Record<ConfidenceTone, { text: string; label: string; Icon: typeof CheckCircle2 }> = {
  high: {
    text: 'text-success',
    label: 'Confident',
    Icon: CheckCircle2,
  },
  low: {
    text: 'text-warning',
    label: 'Check this',
    Icon: ShieldAlert,
  },
  missing: {
    text: 'text-muted-foreground',
    label: 'Not detected',
    Icon: HelpCircle,
  },
};

interface ConfidenceIndicatorProps {
  field: ReceiptField | null | undefined;
  /** Show the numeric score (e.g. "82%") alongside the dot. */
  showScore?: boolean;
  className?: string;
}

/**
 * A small dot + label that tells the user how much to trust an extracted value.
 * Reused next to every detected field so the review screen stays consistent.
 */
export function ConfidenceIndicator({ field, showScore = true, className }: ConfidenceIndicatorProps) {
  const tone = confidenceTone(field);
  const { text, label, Icon } = toneStyles[tone];
  const score = field?.confidence;

  return (
    <span
      className={cn('inline-flex items-center gap-1.5 text-xs font-medium', text, className)}
      title={tone === 'low' ? 'Low confidence — please verify this value' : label}
    >
      <Icon className="h-3.5 w-3.5" aria-hidden="true" />
      {tone === 'high' && showScore && score !== null && score !== undefined
        ? `${score}%`
        : label}
    </span>
  );
}

interface ConfidenceFieldProps {
  label: string;
  field: ReceiptField | null | undefined;
  /** Optional formatter for the value (e.g. currency, date). */
  format?: (value: string | number | null) => string;
  /** Render the value large, as a headline figure (e.g. total). */
  emphasis?: boolean;
}

/**
 * One labelled extracted field: its value plus a confidence indicator. Values the
 * pipeline is unsure about get a soft amber ring so the user knows to double-check
 * them. Missing values show a quiet placeholder rather than an invented default.
 */
export function ConfidenceField({ label, field, format, emphasis }: ConfidenceFieldProps) {
  const tone = confidenceTone(field);
  const value = field?.value ?? null;

  return (
    <div
      className={cn(
        'flex flex-col gap-1 rounded-lg border px-3 py-2.5',
        tone === 'low'
          ? 'border-warning/50 bg-warning/5'
          : tone === 'missing'
            ? 'border-border bg-surface-2/40'
            : 'border-border',
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-medium text-muted-foreground">{label}</span>
        <ConfidenceIndicator field={field} />
      </div>
      {value === null || value === undefined || value === '' ? (
        <span className="text-sm text-muted-foreground/70">—</span>
      ) : (
        <span
          className={cn(
            'font-semibold tabular-nums',
            emphasis ? 'text-xl' : 'text-sm',
            tone === 'low' && 'text-warning',
          )}
        >
          {format ? format(value) : String(value)}
        </span>
      )}
    </div>
  );
}
