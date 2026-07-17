import { type HTMLAttributes } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

/**
 * Animated, accessible progress bar. Reusable wherever Nova shows a completion
 * ratio (budgets now; goals/analytics later). The fill width transitions smoothly,
 * respects reduced-motion, and is surfaced to assistive tech via
 * {@code role="progressbar"} with live value bounds.
 */
const progressVariants = cva('relative w-full overflow-hidden rounded-full bg-surface-2', {
  variants: {
    size: {
      sm: 'h-1.5',
      md: 'h-2.5',
      lg: 'h-3.5',
    },
  },
  defaultVariants: {
    size: 'md',
  },
});

const fillVariants = cva(
  'h-full rounded-full transition-[width] duration-500 ease-out motion-reduce:transition-none',
  {
    variants: {
      tone: {
        primary: 'bg-primary',
        success: 'bg-success',
        warning: 'bg-warning',
        danger: 'bg-danger',
      },
    },
    defaultVariants: {
      tone: 'primary',
    },
  },
);

export interface ProgressProps
  extends HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof progressVariants> {
  /** Completion ratio, 0–100. Clamped for display; the raw value is announced. */
  value: number;
  /** Fill color, mirroring the badge/status palette. */
  tone?: VariantProps<typeof fillVariants>['tone'];
  /** Accessible label describing what the bar measures. */
  label?: string;
}

export function Progress({ value, tone, size, label, className, ...props }: ProgressProps) {
  const clamped = Math.max(0, Math.min(100, Number.isFinite(value) ? value : 0));
  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(clamped * 10) / 10}
      className={cn(progressVariants({ size }), className)}
      {...props}
    >
      <div
        className={cn(fillVariants({ tone }))}
        style={{ width: `${clamped}%` }}
      />
    </div>
  );
}
