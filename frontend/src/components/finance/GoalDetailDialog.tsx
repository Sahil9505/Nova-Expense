import { useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/loading-state';
import { Dialog } from '@/components/ui/dialog';
import { ContributionDialog } from '@/components/finance/ContributionDialog';
import {
  GOAL_STATUS_LABELS,
  goalStatusBadgeVariant,
  goalTypeIcon,
} from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';
import { goalsApi } from '@/lib/api';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import type { Goal } from '@/types';

interface GoalDetailDialogProps {
  open: boolean;
  onClose: () => void;
  goal: Goal | null;
}

export function GoalDetailDialog({ open, onClose, goal }: GoalDetailDialogProps) {
  const { data: currentUser } = useCurrentUser();
  const [contributing, setContributing] = useState(false);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['goals', 'detail', goal?.id],
    queryFn: () => goalsApi.get(goal!.id),
    enabled: open && Boolean(goal),
  });

  const currency = currentUser?.preferredCurrency ?? 'USD';

  // Oldest-first timeline so the progress reads chronologically.
  const timeline = useMemo(() => {
    if (!data) return [];
    return [...data.contributions].sort((a, b) => a.contributedAt.localeCompare(b.contributedAt));
  }, [data]);

  const summary = data?.goal.progress;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={goal?.name ?? 'Goal'}
      description="Progress, status, and contribution history."
      className="sm:max-w-2xl"
      footer={
        <div className="flex justify-end">
          <Button onClick={onClose}>Close</Button>
        </div>
      }
    >
      {isLoading ? (
        <div className="flex flex-col gap-4">
          <Skeleton className="h-24 w-full rounded-lg" />
          <Skeleton className="h-40 w-full rounded-lg" />
        </div>
      ) : isError || !data ? (
        <div className="rounded-lg border border-border bg-surface p-6 text-sm">
          <p className="font-medium">We couldn't load this goal.</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => refetch()}>
            Try again
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-5">
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between gap-3">
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/15 text-primary">
                  {(() => {
                    const Icon = goalTypeIcon(data.goal.type);
                    return <Icon className="h-5 w-5" aria-hidden="true" />;
                  })()}
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{data.goal.name}</p>
                  <p className="text-xs text-muted-foreground">{GOAL_STATUS_LABELS[data.goal.type]}</p>
                </div>
              </div>
              <Badge variant={goalStatusBadgeVariant(summary?.status ?? 'NOT_STARTED')}>
                {GOAL_STATUS_LABELS[summary?.status ?? 'NOT_STARTED']}
              </Badge>
            </div>

            <div className="grid grid-cols-3 gap-3 text-sm">
              <div>
                <p className="text-xs text-muted-foreground">Current</p>
                <p className="font-semibold tabular-nums">{formatCurrency(data.goal.currentAmount, currency)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Target</p>
                <p className="font-semibold tabular-nums">{formatCurrency(data.goal.targetAmount, currency)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Remaining</p>
                <p className="font-semibold tabular-nums">
                  {formatCurrency(summary?.remainingAmount ?? 0, currency)}
                </p>
              </div>
            </div>

            <Progress
              value={summary?.percentageComplete ?? 0}
              tone={
                goalStatusBadgeVariant(summary?.status ?? 'NOT_STARTED') === 'success'
                  ? 'success'
                  : 'primary'
              }
              label={`${data.goal.name}: ${summary?.percentageComplete ?? 0}% complete`}
            />

            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
              <span>
                Target date:{' '}
                <span className="font-medium text-foreground">
                  {new Date(data.goal.targetDate).toLocaleDateString('en-US', {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                  })}
                </span>
              </span>
              {summary?.estimatedCompletionDate ? (
                <span>
                  Est. completion:{' '}
                  <span className="font-medium text-foreground">
                    {new Date(summary.estimatedCompletionDate).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                    })}
                  </span>
                </span>
              ) : null}
            </div>

            {data.goal.description ? (
              <p className="text-sm text-muted-foreground">{data.goal.description}</p>
            ) : null}
          </div>

          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold">Contribution history</h3>
            <Button size="sm" variant="outline" onClick={() => setContributing(true)}>
              <Plus className="h-3.5 w-3.5" aria-hidden="true" />
              Add
            </Button>
          </div>

          {timeline.length === 0 ? (
            <p className="rounded-lg border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
              No contributions yet. Add one to start tracking progress.
            </p>
          ) : (
            <ul className="flex flex-col gap-2">
              {timeline.map((contribution) => (
                <li
                  key={contribution.id}
                  className="flex items-center justify-between gap-3 rounded-lg border border-border bg-surface-2 px-3 py-2.5"
                >
                  <div className="min-w-0">
                    <p className="text-sm font-medium tabular-nums">
                      {formatCurrency(contribution.amount, currency)}
                    </p>
                    {contribution.note ? (
                      <p className="truncate text-xs text-muted-foreground">{contribution.note}</p>
                    ) : null}
                  </div>
                  <span className="shrink-0 text-xs text-muted-foreground">
                    {new Date(contribution.contributedAt).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                    })}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {goal ? (
        <ContributionDialog open={contributing} onClose={() => setContributing(false)} goal={goal} />
      ) : null}
    </Dialog>
  );
}
