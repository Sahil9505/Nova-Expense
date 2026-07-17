import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { cn, formatCurrency, formatPercent } from '@/lib/utils';
import type { StatMetric } from '@/types';

const trendConfig = {
  up: { Icon: ArrowUpRight, pill: 'bg-success/12 text-success' },
  down: { Icon: ArrowDownRight, pill: 'bg-danger/12 text-danger' },
  flat: { Icon: Minus, pill: 'bg-surface-2 text-muted-foreground' },
} as const;

export function StatCard({ metric }: { metric: StatMetric }) {
  const { Icon, pill } = trendConfig[metric.trendDirection];
  const formatted =
    metric.format === 'currency'
      ? formatCurrency(metric.value, metric.currency)
      : metric.format === 'percent'
        ? `${metric.value}%`
        : metric.value.toLocaleString();

  return (
    <Card className="glass-lift group p-5">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-muted-foreground">{metric.label}</p>
        <span
          className={cn(
            'flex items-center gap-0.5 rounded-full px-2 py-0.5 text-xs font-semibold tabular-nums',
            pill,
          )}
        >
          <Icon className="h-3.5 w-3.5" aria-hidden="true" />
          {formatPercent(metric.trend)}
        </span>
      </div>
      <p className="mt-3 text-2xl font-bold tracking-tight tabular-nums">{formatted}</p>
      <p className="mt-1 text-xs text-muted-foreground">{metric.caption}</p>
    </Card>
  );
}
