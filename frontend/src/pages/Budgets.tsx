import { useMemo, useState } from 'react';
import { Pencil, Plus, Target, Trash2 } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/empty-state';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/loading-state';
import { StatCard } from '@/components/ui/stat-card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useToast } from '@/components/ui/toast';
import { BudgetFormDialog } from '@/components/finance/BudgetFormDialog';
import { BUDGET_PERIOD_LABELS, budgetStatusBadgeVariant, categoryIcon, colorOf } from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';
import { useBudgets, useBudgetSummary, useDeleteBudget, useUpdateBudget } from '@/hooks/useBudgets';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { ApiError } from '@/lib/api';
import type { Budget, BudgetMetrics, BudgetWithMetrics, StatMetric } from '@/types';

export function Budgets() {
  const { toast } = useToast();
  const listQuery = useBudgets();
  const summaryQuery = useBudgetSummary();
  const deleteBudget = useDeleteBudget();
  const updateBudget = useUpdateBudget();
  const { data: currentUser } = useCurrentUser();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Budget | null>(null);
  const [confirming, setConfirming] = useState<Budget | null>(null);

  const currency = currentUser?.preferredCurrency ?? 'USD';

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  const openEdit = (budget: Budget) => {
    setEditing(budget);
    setDialogOpen(true);
  };

  const confirmDelete = async () => {
    if (!confirming) return;
    try {
      await deleteBudget.mutateAsync(confirming.id);
      toast({ title: 'Budget deleted', description: 'Its history is kept.', tone: 'success' });
    } catch (error) {
      toast({
        title: 'Could not delete budget',
        description: error instanceof ApiError ? error.message : 'Please try again.',
        tone: 'danger',
      });
    } finally {
      setConfirming(null);
    }
  };

  const toggleActive = async (budget: Budget, active: boolean) => {
    try {
      await updateBudget.mutateAsync({ id: budget.id, payload: { active } });
      toast({ title: active ? 'Budget reactivated' : 'Budget deactivated', tone: 'success' });
    } catch (error) {
      toast({
        title: active ? 'Could not reactivate budget' : 'Could not deactivate budget',
        description: error instanceof ApiError ? error.message : 'Please try again.',
        tone: 'danger',
      });
    }
  };

  // Index the live metrics by budget id so each card can overlay spent/remaining/status.
  // The summary is the single source of truth for intelligence; fall back to the bare
  // list (no metrics) only if the summary is still loading or fails.
  const metricsById = useMemo(() => {
    const map = new Map<string, BudgetMetrics>();
    (summaryQuery.data?.budgets ?? []).forEach((entry: BudgetWithMetrics) => {
      map.set(entry.budget.id, entry.metrics);
    });
    return map;
  }, [summaryQuery.data]);

  const budgets = useMemo<Budget[]>(() => {
    if (summaryQuery.data) {
      return summaryQuery.data.budgets.map((entry) => entry.budget);
    }
    return listQuery.data ?? [];
  }, [summaryQuery.data, listQuery.data]);

  const summary = summaryQuery.data;

  const summaryMetrics: StatMetric[] = summary
    ? [
        {
          id: 'active',
          label: 'Active Budgets',
          value: summary.activeBudgets,
          format: 'number',
          trend: 0,
          trendDirection: 'flat',
          caption: `${summary.warningCount} warning · ${summary.exceededCount} exceeded`,
        },
        {
          id: 'budgeted',
          label: 'Total Budgeted',
          value: summary.totalBudgeted,
          format: 'currency',
          currency,
          trend: 0,
          trendDirection: 'flat',
          caption: 'This period',
        },
        {
          id: 'spent',
          label: 'Total Spent',
          value: summary.totalSpent,
          format: 'currency',
          currency,
          trend: 0,
          trendDirection: 'flat',
          caption: 'Across active budgets',
        },
        {
          id: 'remaining',
          label: 'Remaining Budget',
          value: summary.totalRemaining,
          format: 'currency',
          currency,
          trend: 0,
          trendDirection: 'flat',
          caption: 'Left to spend',
        },
      ]
    : [];

  const isLoading = listQuery.isLoading || summaryQuery.isLoading;
  const isError = listQuery.isError && summaryQuery.isError;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Budgets</h2>
          <p className="text-sm text-muted-foreground">
            Set spending limits and keep your money on track.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          Create budget
        </Button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-28 w-full rounded-lg" />
          ))}
        </div>
      ) : summaryMetrics.length > 0 ? (
        <section aria-label="Budget summary" className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {summaryMetrics.map((metric) => (
            <StatCard key={metric.id} metric={metric} />
          ))}
        </section>
      ) : null}

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-48 w-full rounded-lg" />
          ))}
        </div>
      ) : isError ? (
        <div className="rounded-lg border border-border bg-surface p-6 text-sm">
          <p className="font-medium">We couldn't load your budgets.</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => listQuery.refetch()}>
            Try again
          </Button>
        </div>
      ) : budgets.length === 0 ? (
        <EmptyState
          icon={Target}
          title="No budgets yet"
          description="Start planning your finances by creating your first budget."
          action={
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" aria-hidden="true" />
              Create budget
            </Button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {budgets.map((budget) => {
            const metrics = metricsById.get(budget.id);
            const category = budget.category;
            const CategoryIcon = category ? categoryIcon(category.icon) : Target;
            const color = category ? colorOf(category.color) : '#94A3B8';
            const percentage = metrics ? metrics.percentageUsed : 0;
            const spent = metrics ? metrics.spent : 0;
            const remaining = metrics ? metrics.remaining : budget.amount;
            const status = metrics?.status ?? 'HEALTHY';
            return (
              <Card key={budget.id} className="flex flex-col gap-4 p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <div
                      className="flex h-11 w-11 items-center justify-center rounded-xl"
                      style={{ backgroundColor: `${color}22`, color }}
                    >
                      <CategoryIcon className="h-5 w-5" aria-hidden="true" />
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">{budget.name}</p>
                      <p className="truncate text-xs text-muted-foreground">
                        {category ? category.name : 'Overall budget'}
                      </p>
                    </div>
                  </div>
                  <div className="flex flex-shrink-0 flex-col items-end gap-1.5">
                    {budget.active ? (
                      <Badge variant="success">Active</Badge>
                    ) : (
                      <Badge variant="warning">Inactive</Badge>
                    )}
                    <Badge variant={budgetStatusBadgeVariant(status)}>{status}</Badge>
                  </div>
                </div>

                <div>
                  <p className="text-2xl font-bold tabular-nums">
                    {formatCurrency(budget.amount, currency)}
                  </p>
                  <p className="text-xs text-muted-foreground">{BUDGET_PERIOD_LABELS[budget.period]}</p>
                </div>

                {budget.description ? (
                  <p className="line-clamp-2 text-xs text-muted-foreground">{budget.description}</p>
                ) : null}

                <div className="flex flex-col gap-2">
                  <Progress
                    value={percentage}
                    tone={budgetStatusBadgeVariant(status)}
                    label={`${budget.name}: ${percentage}% used`}
                  />
                  <div className="flex items-center justify-between text-xs tabular-nums">
                    <span className="text-muted-foreground">
                      {formatCurrency(spent, currency)} spent
                    </span>
                    <span className="font-medium">
                      {formatCurrency(remaining, currency)} left
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <Button variant="outline" size="sm" onClick={() => openEdit(budget)}>
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                    Edit
                  </Button>
                  {budget.active ? (
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => setConfirming(budget)}
                    >
                      <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                      Delete
                    </Button>
                  ) : (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => toggleActive(budget, true)}
                    >
                      Reactivate
                    </Button>
                  )}
                </div>
                {budget.active ? (
                  <button
                    type="button"
                    onClick={() => toggleActive(budget, false)}
                    className="text-left text-xs text-muted-foreground underline-offset-2 hover:text-foreground hover:underline"
                  >
                    Deactivate instead
                  </button>
                ) : null}
              </Card>
            );
          })}
        </div>
      )}

      <BudgetFormDialog open={dialogOpen} onClose={() => setDialogOpen(false)} budget={editing} />

      <ConfirmDialog
        open={Boolean(confirming)}
        title="Delete this budget?"
        description={
          confirming
            ? `${confirming.name} will be deactivated and hidden from active views, but its history is preserved.`
            : undefined
        }
        confirmLabel="Delete"
        loading={deleteBudget.isPending}
        onConfirm={confirmDelete}
        onClose={() => setConfirming(null)}
      />
    </div>
  );
}
