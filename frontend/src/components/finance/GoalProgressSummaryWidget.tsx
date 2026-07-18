import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/loading-state';
import { useAnalyticsGoals } from '@/hooks/useAnalytics';
import { goalTypeIcon, GOAL_STATUS_LABELS, goalStatusTone } from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';

/**
 * Goal progress summary from the Analytics domain: a totals strip plus a progress bar
 * per active goal. Reuses the Goal Intelligence engine via the analytics goal endpoint.
 */
export function GoalProgressSummaryWidget({ title = 'Goal Progress' }: { title?: string }) {
  const query = useAnalyticsGoals();
  const summary = query.data?.goalSummary;

  return (
    <Card className="flex flex-col">
      <CardHeader>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <CardTitle>{title}</CardTitle>
          {summary ? (
            <Badge variant="outline">
              {summary.activeGoals} active · {summary.achievedGoals} achieved · {summary.overdueGoals} overdue
            </Badge>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {query.isLoading ? (
          <Skeleton className="h-40 w-full rounded-lg" />
        ) : !summary || summary.goals.length === 0 ? (
          <p className="text-sm text-muted-foreground">No goals yet.</p>
        ) : (
          <>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div>
                <p className="text-lg font-bold tabular-nums">{formatCurrency(summary.totalTarget)}</p>
                <p className="text-xs text-muted-foreground">Target</p>
              </div>
              <div>
                <p className="text-lg font-bold tabular-nums">{formatCurrency(summary.totalCurrent)}</p>
                <p className="text-xs text-muted-foreground">Saved</p>
              </div>
              <div>
                <p className="text-lg font-bold tabular-nums">{summary.overallPercent.toFixed(0)}%</p>
                <p className="text-xs text-muted-foreground">Complete</p>
              </div>
            </div>
            <ul className="flex flex-col gap-3">
              {summary.goals
                .filter((g) => g.goal.active)
                .slice(0, 5)
                .map((g) => {
                  const Icon = goalTypeIcon(g.goal.type);
                  return (
                    <li key={g.goal.id} className="flex flex-col gap-1">
                      <div className="flex items-center justify-between text-sm">
                        <span className="flex items-center gap-2 font-medium">
                          <Icon className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                          {g.goal.name}
                        </span>
                        <Badge variant={goalStatusTone(g.progress.status)}>
                          {GOAL_STATUS_LABELS[g.progress.status]}
                        </Badge>
                      </div>
                      <Progress
                        value={Number(g.progress.percentageComplete)}
                        tone={goalStatusTone(g.progress.status)}
                        label={`${g.goal.name} progress`}
                      />
                    </li>
                  );
                })}
            </ul>
          </>
        )}
      </CardContent>
    </Card>
  );
}
