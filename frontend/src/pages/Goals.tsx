import { useMemo, useState } from 'react';
import { Eye, Pause, Pencil, PiggyBank, Play, Plus, Trash2 } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/empty-state';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/loading-state';
import { StatCard } from '@/components/ui/stat-card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useToast } from '@/components/ui/toast';
import { GoalFormDialog } from '@/components/finance/GoalFormDialog';
import { ContributionDialog } from '@/components/finance/ContributionDialog';
import { GoalDetailDialog } from '@/components/finance/GoalDetailDialog';
import {
  GOAL_STATUS_LABELS,
  GOAL_TYPE_LABELS,
  goalStatusBadgeVariant,
  goalTypeIcon,
} from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';
import {
  useDeleteGoal,
  useGoals,
  useGoalSummary,
  useUpdateGoal,
} from '@/hooks/useGoals';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { ApiError } from '@/lib/api';
import type { Goal, GoalWithProgress, StatMetric } from '@/types';

export function Goals() {
  const { toast } = useToast();
  const listQuery = useGoals();
  const summaryQuery = useGoalSummary();
  const deleteGoal = useDeleteGoal();
  const updateGoal = useUpdateGoal();
  const { data: currentUser } = useCurrentUser();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Goal | null>(null);
  const [contributing, setContributing] = useState<Goal | null>(null);
  const [details, setDetails] = useState<Goal | null>(null);
  const [confirming, setConfirming] = useState<Goal | null>(null);

  const currency = currentUser?.preferredCurrency ?? 'USD';

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  const openEdit = (goal: Goal) => {
    setEditing(goal);
    setDialogOpen(true);
  };

  const confirmDelete = async () => {
    if (!confirming) return;
    try {
      await deleteGoal.mutateAsync(confirming.id);
      toast({ title: 'Goal deleted', description: 'Its contribution history is kept.', tone: 'success' });
    } catch (error) {
      toast({
        title: 'Could not delete goal',
        description: error instanceof ApiError ? error.message : 'Please try again.',
        tone: 'danger',
      });
    } finally {
      setConfirming(null);
    }
  };

  const toggleActive = async (goal: Goal, active: boolean) => {
    try {
      await updateGoal.mutateAsync({ id: goal.id, payload: { active } });
      toast({ title: active ? 'Goal reactivated' : 'Goal deactivated', tone: 'success' });
    } catch (error) {
      toast({
        title: active ? 'Could not reactivate goal' : 'Could not deactivate goal',
        description: error instanceof ApiError ? error.message : 'Please try again.',
        tone: 'danger',
      });
    }
  };

  const togglePause = async (goal: Goal, paused: boolean) => {
    try {
      await updateGoal.mutateAsync({ id: goal.id, payload: { paused } });
      toast({ title: paused ? 'Goal paused' : 'Goal resumed', tone: 'success' });
    } catch (error) {
      toast({
        title: paused ? 'Could not pause goal' : 'Could not resume goal',
        description: error instanceof ApiError ? error.message : 'Please try again.',
        tone: 'danger',
      });
    }
  };

  const summary = summaryQuery.data;

  const summaryMetrics: StatMetric[] = summary
    ? [
        {
          id: 'active',
          label: 'Active Goals',
          value: summary.activeGoals,
          format: 'number',
          trend: 0,
          trendDirection: 'flat',
          caption: `${summary.achievedGoals} achieved · ${summary.pausedGoals} paused`,
        },
        {
          id: 'saved',
          label: 'Total Saved',
          value: summary.totalCurrent,
          format: 'currency',
          currency,
          trend: 0,
          trendDirection: 'flat',
          caption: `${summary.overallPercent}% of target`,
        },
        {
          id: 'target',
          label: 'Total Target',
          value: summary.totalTarget,
          format: 'currency',
          currency,
          trend: 0,
          trendDirection: 'flat',
          caption: 'Across active goals',
        },
        {
          id: 'remaining',
          label: 'Remaining',
          value: summary.totalRemaining,
          format: 'currency',
          currency,
          trend: 0,
          trendDirection: 'flat',
          caption: 'Left to reach targets',
        },
      ]
    : [];

  const goals = useMemo<GoalWithProgress[]>(() => {
    const list = (summaryQuery.data?.goals ?? []).map((entry) => ({
      goal: entry.goal,
      progress: entry.progress,
    }));
    if (list.length > 0) return list;
    return (listQuery.data ?? []).map((goal) => ({ goal, progress: goal.progress }));
  }, [summaryQuery.data, listQuery.data]);

  const isLoading = listQuery.isLoading || summaryQuery.isLoading;
  const isError = listQuery.isError && summaryQuery.isError;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Goals</h2>
          <p className="text-sm text-muted-foreground">
            Set long-term objectives and track your progress over time.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          Create goal
        </Button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-28 w-full rounded-lg" />
          ))}
        </div>
      ) : summaryMetrics.length > 0 ? (
        <section aria-label="Goal summary" className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
          <p className="font-medium">We couldn't load your goals.</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => listQuery.refetch()}>
            Try again
          </Button>
        </div>
      ) : goals.length === 0 ? (
        <EmptyState
          icon={PiggyBank}
          title="No goals yet"
          description="Start planning your finances by creating your first goal."
          action={
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" aria-hidden="true" />
              Create goal
            </Button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {goals.map(({ goal, progress }) => {
            const TypeIcon = goalTypeIcon(goal.type);
            const status = progress.status;
            return (
              <Card key={goal.id} className="flex flex-col gap-4 p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/15 text-primary">
                      <TypeIcon className="h-5 w-5" aria-hidden="true" />
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">{goal.name}</p>
                      <p className="truncate text-xs text-muted-foreground">
                        {GOAL_TYPE_LABELS[goal.type]}
                      </p>
                    </div>
                  </div>
                  <div className="flex flex-shrink-0 flex-col items-end gap-1.5">
                    {goal.active ? (
                      <Badge variant="success">Active</Badge>
                    ) : (
                      <Badge variant="warning">Inactive</Badge>
                    )}
                    <Badge variant={goalStatusBadgeVariant(status)}>{GOAL_STATUS_LABELS[status]}</Badge>
                  </div>
                </div>

                <div>
                  <p className="text-2xl font-bold tabular-nums">
                    {formatCurrency(goal.currentAmount, currency)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    of {formatCurrency(goal.targetAmount, currency)} by{' '}
                    {new Date(goal.targetDate).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                    })}
                  </p>
                </div>

                {goal.description ? (
                  <p className="line-clamp-2 text-xs text-muted-foreground">{goal.description}</p>
                ) : null}

                <div className="flex flex-col gap-2">
                  <Progress
                    value={progress.percentageComplete}
                    tone={goalStatusBadgeVariant(status) === 'success' ? 'success' : 'primary'}
                    label={`${goal.name}: ${progress.percentageComplete}% complete`}
                  />
                  <div className="flex items-center justify-between text-xs tabular-nums">
                    <span className="text-muted-foreground">
                      {progress.percentageComplete}% complete
                    </span>
                    <span className="font-medium">
                      {formatCurrency(progress.remainingAmount, currency)} left
                    </span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <Button variant="outline" size="sm" onClick={() => setDetails(goal)}>
                    <Eye className="h-3.5 w-3.5" aria-hidden="true" />
                    Details
                  </Button>
                  <Button variant="outline" size="sm" onClick={() => setContributing(goal)}>
                    <Plus className="h-3.5 w-3.5" aria-hidden="true" />
                    Add
                  </Button>
                  <Button variant="outline" size="sm" onClick={() => openEdit(goal)}>
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                    Edit
                  </Button>
                  {goal.paused ? (
                    <Button variant="outline" size="sm" onClick={() => togglePause(goal, false)}>
                      <Play className="h-3.5 w-3.5" aria-hidden="true" />
                      Resume
                    </Button>
                  ) : (
                    <Button variant="outline" size="sm" onClick={() => togglePause(goal, true)}>
                      <Pause className="h-3.5 w-3.5" aria-hidden="true" />
                      Pause
                    </Button>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  {goal.active ? (
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => setConfirming(goal)}
                    >
                      <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                      Delete
                    </Button>
                  ) : (
                    <Button variant="outline" size="sm" onClick={() => toggleActive(goal, true)}>
                      Reactivate
                    </Button>
                  )}
                  {goal.active ? (
                    <button
                      type="button"
                      onClick={() => toggleActive(goal, false)}
                      className="text-left text-xs text-muted-foreground underline-offset-2 hover:text-foreground hover:underline"
                    >
                      Deactivate instead
                    </button>
                  ) : null}
                </div>
              </Card>
            );
          })}
        </div>
      )}

      <GoalFormDialog open={dialogOpen} onClose={() => setDialogOpen(false)} goal={editing} />
      <ContributionDialog
        open={Boolean(contributing)}
        onClose={() => setContributing(null)}
        goal={contributing}
      />
      <GoalDetailDialog open={Boolean(details)} onClose={() => setDetails(null)} goal={details} />

      <ConfirmDialog
        open={Boolean(confirming)}
        title="Delete this goal?"
        description={
          confirming
            ? `${confirming.name} will be deactivated and hidden from active views, but its contribution history is preserved.`
            : undefined
        }
        confirmLabel="Delete"
        loading={deleteGoal.isPending}
        onConfirm={confirmDelete}
        onClose={() => setConfirming(null)}
      />
    </div>
  );
}
