import { useMemo, useState } from 'react';
import { FileDown, CalendarDays } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { StatCard } from '@/components/ui/stat-card';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/loading-state';
import { useAnalyticsOverview, useAnalyticsExport } from '@/hooks/useAnalytics';
import { useAccounts } from '@/hooks/useAccounts';
import { useCategories } from '@/hooks/useCategories';
import { colorOf, formatTransactionDate } from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';
import type { AnalyticsFilter, AnalyticsPeriod, StatMetric } from '@/types';
import { CashFlowWidget } from '@/components/finance/CashFlowWidget';
import { CategoryBreakdownWidget } from '@/components/finance/CategoryBreakdownWidget';
import { BudgetDistributionWidget } from '@/components/finance/BudgetDistributionWidget';
import { GoalProgressSummaryWidget } from '@/components/finance/GoalProgressSummaryWidget';

const PERIODS: AnalyticsPeriod[] = ['weekly', 'monthly', 'yearly', 'custom'];

export function Analytics() {
  const [period, setPeriod] = useState<AnalyticsPeriod>('monthly');
  const [accountId, setAccountId] = useState<string>('');
  const [categoryId, setCategoryId] = useState<string>('');
  const [from, setFrom] = useState<string>('');
  const [to, setTo] = useState<string>('');

  const filter: AnalyticsFilter = useMemo(
    () => ({
      period,
      accountId: accountId || undefined,
      categoryId: categoryId || undefined,
      from: period === 'custom' && from ? `${from}T00:00:00Z` : undefined,
      to: period === 'custom' && to ? `${to}T23:59:59Z` : undefined,
    }),
    [period, accountId, categoryId, from, to],
  );

  const overview = useAnalyticsOverview(filter);
  const exportMutation = useAnalyticsExport();
  const accounts = useAccounts();
  const categories = useCategories();

  const metrics: StatMetric[] = useMemo(() => {
    const s = overview.data?.spendingOverview;
    const currency = overview.data?.currency ?? 'USD';
    if (!s) return [];
    return [
      {
        id: 'income',
        label: 'Income',
        value: s.income,
        format: 'currency',
        currency,
        trend: 0,
        trendDirection: 'flat',
        caption: 'In this period',
      },
      {
        id: 'expenses',
        label: 'Expenses',
        value: s.expenses,
        format: 'currency',
        currency,
        trend: 0,
        trendDirection: 'flat',
        caption: 'In this period',
      },
      {
        id: 'net',
        label: 'Net Cash Flow',
        value: s.netCashFlow,
        format: 'currency',
        currency,
        trend: s.savingsRatePct,
        trendDirection: s.netCashFlow >= 0 ? 'up' : 'down',
        caption: 'Net of the period',
      },
      {
        id: 'savings',
        label: 'Savings Rate',
        value: s.savingsRatePct,
        format: 'percent',
        currency,
        trend: 0,
        trendDirection: 'flat',
        caption: `${s.transactionCount} transactions`,
      },
    ];
  }, [overview.data]);

  const isLoading = overview.isLoading;
  const isError = overview.isError;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Analytics</h2>
          <p className="text-sm text-muted-foreground">Understand where your money goes and how it changes over time.</p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={exportMutation.isPending}
            onClick={() => exportMutation.mutate({ format: 'CSV', filter })}
          >
            <FileDown className="h-4 w-4" aria-hidden="true" />
            Export CSV
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={exportMutation.isPending}
            onClick={() => exportMutation.mutate({ format: 'PDF', filter })}
          >
            <FileDown className="h-4 w-4" aria-hidden="true" />
            Export PDF
          </Button>
        </div>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1.5">
            <span className="text-sm font-medium text-foreground">Period</span>
            <div className="flex gap-1 rounded-lg bg-surface-2 p-1">
              {PERIODS.map((p) => (
                <button
                  key={p}
                  type="button"
                  onClick={() => setPeriod(p)}
                  className={
                    'rounded-md px-3 py-1.5 text-sm font-medium transition-all duration-200 ease-premium ' +
                    (period === p
                      ? 'bg-primary text-primary-foreground shadow-sm'
                      : 'text-muted-foreground hover:text-foreground')
                  }
                >
                  {p[0].toUpperCase() + p.slice(1)}
                </button>
              ))}
            </div>
          </div>
          <Select
            label="Account"
            className="w-44"
            placeholder="All accounts"
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
            options={(accounts.data ?? []).map((a) => ({ value: a.id, label: a.name }))}
          />
          <Select
            label="Category"
            className="w-44"
            placeholder="All categories"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            options={(categories.data ?? []).map((c) => ({ value: c.id, label: c.name }))}
          />
          {period === 'custom' ? (
            <>
              <div className="flex flex-col gap-1.5">
                <span className="text-sm font-medium text-foreground">From</span>
                <input
                  type="date"
                  value={from}
                  onChange={(e) => setFrom(e.target.value)}
                  className="flex h-10 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <span className="text-sm font-medium text-foreground">To</span>
                <input
                  type="date"
                  value={to}
                  onChange={(e) => setTo(e.target.value)}
                  className="flex h-10 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground"
                />
              </div>
            </>
          ) : null}
        </div>
      </Card>

      {isLoading ? (
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-28 w-full rounded-lg" />
            ))}
          </div>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <Skeleton className="h-80 w-full rounded-lg" />
            <Skeleton className="h-80 w-full rounded-lg" />
          </div>
        </div>
      ) : isError || !overview.data ? (
        <EmptyState
          icon={CalendarDays}
          title="We couldn't load your analytics."
          description="Try adjusting the filters, or refresh the page."
        />
      ) : (
        <>
          <section aria-label="Key metrics" className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {metrics.map((metric) => (
              <StatCard key={metric.id} metric={metric} />
            ))}
          </section>

          <section aria-label="Trends" className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <CashFlowWidget filter={filter} />
            <CategoryBreakdownWidget filter={filter} />
          </section>

          <section aria-label="Budgets and goals" className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <BudgetDistributionWidget />
            <GoalProgressSummaryWidget />
          </section>

          <section aria-label="Top categories" className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Top Spending Categories</CardTitle>
              </CardHeader>
              <CardContent>
                {overview.data.categoryAnalysis.topCategories.length === 0 ? (
                  <EmptyState icon={CalendarDays} title="No spending yet" description="Add expenses to see your top categories." />
                ) : (
                  <ul className="flex flex-col gap-2">
                    {overview.data.categoryAnalysis.topCategories.map((item) => (
                      <li key={item.name} className="flex items-center gap-2 text-sm">
                        <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: colorOf(item.color) }} />
                        <span className="text-muted-foreground">{item.name}</span>
                        <span className="ml-auto font-medium tabular-nums">
                          {formatCurrency(item.amount, overview.data.currency)}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Upcoming Goal Deadlines</CardTitle>
              </CardHeader>
              <CardContent>
                {overview.data.goalAnalytics.upcomingDeadlines.length === 0 ? (
                  <EmptyState icon={CalendarDays} title="No upcoming deadlines" description="Goals without a deadline won't appear here." />
                ) : (
                  <ul className="flex flex-col gap-2">
                    {overview.data.goalAnalytics.upcomingDeadlines.map((g) => (
                      <li key={g.goal.id} className="flex items-center justify-between text-sm">
                        <span className="font-medium">{g.goal.name}</span>
                        <span className="text-muted-foreground">
                          {g.goal.targetDate ? formatTransactionDate(g.goal.targetDate) : '—'} ·{' '}
                          {g.progress.percentageComplete.toFixed(0)}%
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          </section>
        </>
      )}
    </div>
  );
}
