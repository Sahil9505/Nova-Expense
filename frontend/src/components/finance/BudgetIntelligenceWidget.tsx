import { useMemo } from 'react';
import { AlertTriangle, ShieldCheck, ShieldAlert, Target } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/loading-state';
import { useBudgetSummary } from '@/hooks/useBudgets';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { budgetStatusBadgeVariant, BUDGET_STATUS_LABELS } from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';
import type { BudgetStatus, BudgetWithMetrics } from '@/types';

interface BudgetIntelligenceWidgetProps {
  /** Restrict to a single health bucket (e.g. only EXCEEDED budgets). */
  statusFilter?: BudgetStatus;
  /** Section heading shown in the card header. */
  title: string;
  /** Empty-state copy when no budgets match. */
  emptyTitle: string;
  emptyDescription?: string;
  /** Sort by percentage used descending (for "near limit"). */
  sortByUsage?: boolean;
  /** Cap the number of rows shown. */
  limit?: number;
}

const statusGlyph: Record<BudgetStatus, typeof ShieldCheck> = {
  HEALTHY: ShieldCheck,
  WARNING: AlertTriangle,
  EXCEEDED: ShieldAlert,
};

/**
 * Reusable Budget Intelligence panel. Renders the summary + a filtered, sorted list
 * of budgets with live progress and health badges. The Dashboard composes several of
 * these ("near limit", "recently exceeded") from one summary query; the Budgets page
 * reuses the same data shape.
 */
export function BudgetIntelligenceWidget({
  statusFilter,
  title,
  emptyTitle,
  emptyDescription,
  sortByUsage = false,
  limit,
}: BudgetIntelligenceWidgetProps) {
  const { data: currentUser } = useCurrentUser();
  const query = useBudgetSummary();
  const currency = currentUser?.preferredCurrency ?? 'USD';

  const rows = useMemo(() => {
    let list: BudgetWithMetrics[] = query.data?.budgets ?? [];
    if (statusFilter) {
      list = list.filter((entry) => entry.budget.active && entry.metrics.status === statusFilter);
    }
    if (sortByUsage) {
      list = [...list].sort((a, b) => b.metrics.percentageUsed - a.metrics.percentageUsed);
    }
    return limit ? list.slice(0, limit) : list;
  }, [query.data, statusFilter, sortByUsage, limit]);

  if (query.isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{title}</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-24 w-full rounded-lg" />
        </CardContent>
      </Card>
    );
  }

  if (query.isError) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{title}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">Could not load budget data.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {rows.length === 0 ? (
          <EmptyState
            icon={Target}
            title={emptyTitle}
            description={emptyDescription}
            className="border-0 px-0 py-6"
          />
        ) : (
          <ul className="flex flex-col gap-4">
            {rows.map(({ budget, metrics }) => {
              const Glyph = statusGlyph[metrics.status];
              return (
                <li key={budget.id} className="flex flex-col gap-2">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex min-w-0 items-center gap-2">
                      <Glyph
                        className={`h-4 w-4 shrink-0 ${
                          metrics.status === 'HEALTHY'
                            ? 'text-success'
                            : metrics.status === 'WARNING'
                              ? 'text-warning'
                              : 'text-danger'
                        }`}
                        aria-hidden="true"
                      />
                      <span className="truncate text-sm font-medium">{budget.name}</span>
                      <Badge variant={budgetStatusBadgeVariant(metrics.status)}>
                        {BUDGET_STATUS_LABELS[metrics.status]}
                      </Badge>
                    </div>
                    <span className="shrink-0 text-xs tabular-nums text-muted-foreground">
                      {formatCurrency(metrics.spent, currency)} / {formatCurrency(metrics.amount, currency)}
                    </span>
                  </div>
                  <Progress
                    value={metrics.percentageUsed}
                    tone={budgetStatusBadgeVariant(metrics.status)}
                    label={`${budget.name}: ${metrics.percentageUsed}% used`}
                  />
                </li>
              );
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
