import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/loading-state';
import { useAnalyticsBudgets } from '@/hooks/useAnalytics';
import { budgetStatusTone, BUDGET_STATUS_LABELS } from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';

/**
 * Budget distribution summary from the Analytics domain: a health roll-up plus a
 * utilization bar per active budget. Reuses the Budget Intelligence engine via the
 * analytics budget endpoint so figures match the Budgets page exactly.
 */
export function BudgetDistributionWidget({ title = 'Budget Distribution' }: { title?: string }) {
  const query = useAnalyticsBudgets();
  const summary = query.data?.budgetSummary;

  return (
    <Card className="flex flex-col">
      <CardHeader>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <CardTitle>{title}</CardTitle>
          {summary ? (
            <Badge variant="outline">
              {summary.activeBudgets} active · {query.data?.budgetSummary.warningCount} warning ·{' '}
              {query.data?.budgetSummary.exceededCount} exceeded
            </Badge>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {query.isLoading ? (
          <Skeleton className="h-40 w-full rounded-lg" />
        ) : !summary || summary.budgets.length === 0 ? (
          <p className="text-sm text-muted-foreground">No budgets yet.</p>
        ) : (
          <>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div>
                <p className="text-lg font-bold tabular-nums">{formatCurrency(summary.totalBudgeted)}</p>
                <p className="text-xs text-muted-foreground">Budgeted</p>
              </div>
              <div>
                <p className="text-lg font-bold tabular-nums">{formatCurrency(summary.totalSpent)}</p>
                <p className="text-xs text-muted-foreground">Spent</p>
              </div>
              <div>
                <p className="text-lg font-bold tabular-nums">
                  {query.data?.budgetEfficiencyPct.toFixed(0)}%
                </p>
                <p className="text-xs text-muted-foreground">Efficiency</p>
              </div>
            </div>
            <ul className="flex flex-col gap-3">
              {summary.budgets
                .filter((b) => b.budget.active)
                .slice(0, 5)
                .map((b) => (
                  <li key={b.budget.id} className="flex flex-col gap-1">
                    <div className="flex items-center justify-between text-sm">
                      <span className="font-medium">{b.budget.name}</span>
                      <Badge variant={budgetStatusTone(b.metrics.status)}>
                        {BUDGET_STATUS_LABELS[b.metrics.status]}
                      </Badge>
                    </div>
                    <Progress
                      value={Number(b.metrics.percentageUsed)}
                      tone={budgetStatusTone(b.metrics.status)}
                      label={`${b.budget.name} utilization`}
                    />
                  </li>
                ))}
            </ul>
          </>
        )}
      </CardContent>
    </Card>
  );
}
