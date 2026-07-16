import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { cn, formatCurrency, formatPercent } from '@/lib/utils';
import type { StatMetric } from '@/types';

const trendConfig = {
  up: { Icon: ArrowUpRight, className: 'text-success' },
  down: { Icon: ArrowDownRight, className: 'text-danger' },
  flat: { Icon: Minus, className: 'text-muted-foreground' },
} as const;

export function StatCard({ metric }: { metric: StatMetric }) {
  const { Icon, className } = trendConfig[metric.trendDirection];
  const formatted =
    metric.format === 'currency'
      ? formatCurrency(metric.value, metric.currency)
      : metric.format === 'percent'
        ? `${metric.value}%`
        : metric.value.toLocaleString();

  return (
    <Card className="p-5 transition-transform duration-200 hover:-translate-y-0.5">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-muted-foreground">{metric.label}</p>
        <span className={cn('flex items-center gap-0.5 text-sm font-semibold', className)}>
          <Icon className="h-4 w-4" aria-hidden="true" />
          {formatPercent(metric.trend)}
        </span>
      </div>
      <p className="mt-3 text-2xl font-bold tracking-tight tabular-nums">{formatted}</p>
      <p className="mt-1 text-xs text-muted-foreground">{metric.caption}</p>
    </Card>
  );
}
