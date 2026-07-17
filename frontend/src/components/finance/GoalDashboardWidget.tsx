import { useMemo } from 'react';
import { Flag, CheckCircle2, CalendarClock, TrendingUp } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/loading-state';
import { useGoalSummary } from '@/hooks/useGoals';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { goalStatusBadgeVariant, goalTypeIcon } from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';
import type { GoalWithProgress } from '@/types';

type WidgetSelection = 'progress' | 'upcoming' | 'completed';

interface GoalDashboardWidgetProps {
  /** Which slice of the goal summary to render. */
  selection: WidgetSelection;
  title: string;
  emptyTitle: string;
  emptyDescription?: string;
  limit?: number;
}

const selectionGlyph: Record<WidgetSelection, typeof Flag> = {
  progress: TrendingUp,
  upcoming: CalendarClock,
  completed: CheckCircle2,
};

/**
 * Reusable Goal panel for the dashboard. Renders the summary and a filtered, sorted
 * slice of goals (progress leaders, upcoming deadlines, or recently completed) from a
 * single summary query — the same pattern the Budget Intelligence widgets use.
 */
export function GoalDashboardWidget({
  selection,
  title,
  emptyTitle,
  emptyDescription,
  limit,
}: GoalDashboardWidgetProps) {
  const { data: currentUser } = useCurrentUser();
  const query = useGoalSummary();
  const currency = currentUser?.preferredCurrency ?? 'USD';

  const rows = useMemo<GoalWithProgress[]>(() => {
    let list: GoalWithProgress[] = query.data?.goals ?? [];
    if (selection === 'progress') {
      list = list.filter((g) => g.goal.active && g.progress.status !== 'ACHIEVED');
      list = [...list].sort((a, b) => b.progress.percentageComplete - a.progress.percentageComplete);
    } else if (selection === 'upcoming') {
      list = list.filter((g) => g.goal.active && g.progress.status !== 'ACHIEVED');
      list = [...list].sort(
        (a, b) => a.goal.targetDate.localeCompare(b.goal.targetDate),
      );
    } else {
      list = list.filter((g) => g.progress.status === 'ACHIEVED');
      list = [...list].sort((a, b) => b.goal.updatedAt.localeCompare(a.goal.updatedAt));
    }
    return limit ? list.slice(0, limit) : list;
  }, [query.data, selection, limit]);

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
          <p className="text-sm text-muted-foreground">Could not load goal data.</p>
        </CardContent>
      </Card>
    );
  }

  const Glyph = selectionGlyph[selection];

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-3 space-y-0">
        <CardTitle>{title}</CardTitle>
        <Link
          to="/goals"
          className="text-xs font-medium text-primary hover:underline"
          aria-label="View all goals"
        >
          View all
        </Link>
      </CardHeader>
      <CardContent>
        {rows.length === 0 ? (
          <EmptyState
            icon={Glyph}
            title={emptyTitle}
            description={emptyDescription}
            className="border-0 px-0 py-6"
          />
        ) : (
          <ul className="flex flex-col gap-4">
            {rows.map(({ goal, progress }) => {
              const TypeIcon = goalTypeIcon(goal.type);
              return (
                <li key={goal.id} className="flex flex-col gap-2">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex min-w-0 items-center gap-2">
                      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-primary/15 text-primary">
                        <TypeIcon className="h-4 w-4" aria-hidden="true" />
                      </span>
                      <span className="truncate text-sm font-medium">{goal.name}</span>
                      <Badge variant={goalStatusBadgeVariant(progress.status)}>
                        {Math.round(progress.percentageComplete)}%
                      </Badge>
                    </div>
                    <span className="shrink-0 text-xs tabular-nums text-muted-foreground">
                      {formatCurrency(progress.currentAmount, currency)} /{' '}
                      {formatCurrency(progress.targetAmount, currency)}
                    </span>
                  </div>
                  <Progress
                    value={progress.percentageComplete}
                    tone={
                      goalStatusBadgeVariant(progress.status) === 'success' ? 'success' : 'primary'
                    }
                    label={`${goal.name}: ${progress.percentageComplete}% complete`}
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
