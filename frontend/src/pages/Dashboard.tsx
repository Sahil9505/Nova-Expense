import { type CSSProperties } from 'react';
import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight, Receipt } from 'lucide-react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ChartCard } from '@/components/ui/chart-card';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/loading-state';
import { StatCard } from '@/components/ui/stat-card';
import { useDashboardSummary } from '@/hooks/useDashboard';
import { useBudgetSummary } from '@/hooks/useBudgets';
import { BudgetIntelligenceWidget } from '@/components/finance/BudgetIntelligenceWidget';
import { useTheme } from '@/context/ThemeProvider';
import { colorOf } from '@/lib/finance';
import { formatCurrency, formatCompact } from '@/lib/utils';
import type { StatMetric, Transaction } from '@/types';

const typeIcon = (tx: Transaction) => {
  if (tx.type === 'INCOME') return ArrowDownLeft;
  if (tx.type === 'TRANSFER') return ArrowLeftRight;
  return ArrowUpRight;
};

const iconTone = (tx: Transaction) => {
  if (tx.type === 'INCOME') return 'bg-success/15 text-success';
  if (tx.type === 'TRANSFER') return 'bg-primary/15 text-primary';
  return 'bg-surface-2 text-muted-foreground';
};

const amountTone = (tx: Transaction) =>
  tx.type === 'INCOME' ? 'text-success' : 'text-foreground';

const amountPrefix = (tx: Transaction) =>
  tx.type === 'INCOME' ? '+' : tx.type === 'EXPENSE' ? '-' : '';

const titleFor = (tx: Transaction) =>
  tx.merchant || tx.note || (tx.type === 'TRANSFER' ? 'Transfer' : 'Transaction');

const subtitleFor = (tx: Transaction) =>
  tx.type === 'TRANSFER'
    ? `${tx.account?.name ?? 'Account'} → ${tx.destinationAccount?.name ?? 'Account'}`
    : (tx.category?.name ?? '');

export function Dashboard() {
  const { theme } = useTheme();
  const query = useDashboardSummary();
  const budgetSummary = useBudgetSummary();

  const axisColor = theme === 'dark' ? '#94A3B8' : '#475569';
  const gridColor = theme === 'dark' ? '#334155' : '#E2E8F0';
  const tooltipStyle: CSSProperties = {
    backgroundColor: theme === 'dark' ? '#1E293B' : '#FFFFFF',
    border: `1px solid ${gridColor}`,
    borderRadius: '0.5rem',
    color: theme === 'dark' ? '#F8FAFC' : '#0F172A',
    fontSize: '12px',
  };

  if (query.isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-9 w-48 rounded-md" />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-28 w-full rounded-lg" />
          ))}
        </div>
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <Skeleton className="h-80 w-full rounded-lg" />
          <Skeleton className="h-80 w-full rounded-lg" />
        </div>
        <Skeleton className="h-64 w-full rounded-lg" />
      </div>
    );
  }

  if (query.isError || !query.data) {
    return (
      <div className="flex flex-col gap-6">
        <h2 className="text-2xl font-bold tracking-tight">Dashboard</h2>
        <div className="rounded-lg border border-border bg-surface p-6 text-sm">
          <p className="font-medium">We couldn't load your dashboard.</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => query.refetch()}>
            Try again
          </Button>
        </div>
      </div>
    );
  }

  const summary = query.data;
  const currency = summary.currency;
  const savingsRate = summary.monthlyIncome > 0
    ? Math.round((summary.netCashFlow / summary.monthlyIncome) * 100)
    : 0;

  const metrics: StatMetric[] = [
    {
      id: 'balance',
      label: 'Total Balance',
      value: summary.totalBalance,
      format: 'currency',
      currency,
      trend: 0,
      trendDirection: 'flat',
      caption: `${summary.accountsCount} account${summary.accountsCount === 1 ? '' : 's'}`,
    },
    {
      id: 'income',
      label: 'Monthly Income',
      value: summary.monthlyIncome,
      format: 'currency',
      currency,
      trend: 0,
      trendDirection: 'flat',
      caption: 'This month',
    },
    {
      id: 'expenses',
      label: 'Monthly Expenses',
      value: summary.monthlyExpenses,
      format: 'currency',
      currency,
      trend: 0,
      trendDirection: 'flat',
      caption: 'This month',
    },
    {
      id: 'net',
      label: 'Net Cash Flow',
      value: summary.netCashFlow,
      format: 'currency',
      currency,
      trend: savingsRate,
      trendDirection: summary.monthlyIncome > 0 ? (summary.netCashFlow >= 0 ? 'up' : 'down') : 'flat',
      caption: 'Saved this month',
    },
  ];

  const categoryData = summary.categoryBreakdown.map((item) => ({
    category: item.name,
    color: colorOf(item.color),
    amount: item.amount,
  }));
  const totalSpending = categoryData.reduce((sum, entry) => sum + entry.amount, 0);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Dashboard</h2>
          <p className="text-sm text-muted-foreground">A snapshot of your finances this month.</p>
        </div>
        <Badge variant="outline">
          {new Date().toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
        </Badge>
      </div>

      <section aria-label="Key metrics" className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {metrics.map((metric) => (
          <StatCard key={metric.id} metric={metric} />
        ))}
      </section>

      <section aria-label="Trends" className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ChartCard title="Cash Flow" description="Income vs. expenses over time">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={summary.monthlyTrend} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
              <defs>
                <linearGradient id="incomeFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#10B981" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#10B981" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="expenseFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#3B82F6" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#3B82F6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />
              <XAxis dataKey="label" stroke={axisColor} tickLine={false} axisLine={false} fontSize={12} />
              <YAxis
                stroke={axisColor}
                tickLine={false}
                axisLine={false}
                fontSize={12}
                width={48}
                tickFormatter={(value) => formatCompact(Number(value))}
              />
              <Tooltip contentStyle={tooltipStyle} formatter={(value) => formatCurrency(Number(value))} />
              <Area
                type="monotone"
                dataKey="income"
                name="Income"
                stroke="#10B981"
                strokeWidth={2}
                fill="url(#incomeFill)"
              />
              <Area
                type="monotone"
                dataKey="expenses"
                name="Expenses"
                stroke="#3B82F6"
                strokeWidth={2}
                fill="url(#expenseFill)"
              />
            </AreaChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Spending by Category" description="Where your money went">
          {categoryData.length === 0 ? (
            <div className="flex h-full items-center justify-center">
              <p className="text-sm text-muted-foreground">No spending recorded yet this month.</p>
            </div>
          ) : (
            <div className="flex h-full items-center gap-4">
              <div className="relative h-full w-1/2">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={categoryData}
                      dataKey="amount"
                      nameKey="category"
                      innerRadius="62%"
                      outerRadius="92%"
                      paddingAngle={2}
                      stroke="none"
                    >
                      {categoryData.map((entry) => (
                        <Cell key={entry.category} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip contentStyle={tooltipStyle} formatter={(value) => formatCurrency(Number(value))} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                  <span className="text-xs text-muted-foreground">Total</span>
                  <span className="text-sm font-bold tabular-nums">{formatCurrency(totalSpending)}</span>
                </div>
              </div>
              <ul className="flex flex-1 flex-col gap-2">
                {categoryData.map((entry) => (
                  <li key={entry.category} className="flex items-center gap-2 text-sm">
                    <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: entry.color }} />
                    <span className="text-muted-foreground">{entry.category}</span>
                    <span className="ml-auto font-medium tabular-nums">{formatCurrency(entry.amount)}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </ChartCard>
      </section>

      <section aria-label="Budgets" className="flex flex-col gap-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h3 className="text-lg font-semibold tracking-tight">Budgets</h3>
            <p className="text-sm text-muted-foreground">How your spending measures up this period.</p>
          </div>
          {budgetSummary.data ? (
            <Badge variant="outline">
              {budgetSummary.data.activeBudgets} active · {budgetSummary.data.warningCount} warning ·{' '}
              {budgetSummary.data.exceededCount} exceeded
            </Badge>
          ) : null}
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <BudgetIntelligenceWidget
            title="Budget Health"
            statusFilter="WARNING"
            sortByUsage
            limit={4}
            emptyTitle="No budgets near their limit"
            emptyDescription="Budgets at 80–99% of their limit show up here."
          />
          <BudgetIntelligenceWidget
            title="Recently Exceeded"
            statusFilter="EXCEEDED"
            sortByUsage
            limit={4}
            emptyTitle="No exceeded budgets"
            emptyDescription="Budgets over their limit appear here so you can act on them."
          />
          <BudgetIntelligenceWidget
            title="Healthy Budgets"
            statusFilter="HEALTHY"
            limit={4}
            emptyTitle="No healthy budgets yet"
            emptyDescription="Budgets under 80% of their limit show up here."
          />
        </div>
      </section>

      <Card>
        <CardHeader>
          <CardTitle>Recent Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          {summary.recentTransactions.length === 0 ? (
            <EmptyState
              icon={Receipt}
              title="No transactions yet"
              description="Add an account and a transaction to see your recent activity here."
            />
          ) : (
            <ul className="flex flex-col">
              {summary.recentTransactions.map((tx) => {
                const Icon = typeIcon(tx);
                return (
                  <li
                    key={tx.id}
                    className="flex items-center gap-3 border-b border-border py-3 last:border-0"
                  >
                    <span
                      className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${iconTone(tx)}`}
                    >
                      <Icon className="h-4 w-4" aria-hidden="true" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{titleFor(tx)}</p>
                      <p className="truncate text-xs text-muted-foreground">{subtitleFor(tx)}</p>
                    </div>
                    {tx.type === 'TRANSFER' ? (
                      <Badge variant="outline">Transfer</Badge>
                    ) : tx.category?.color ? (
                      <span className="hidden items-center gap-1.5 text-xs text-muted-foreground sm:flex">
                        <span
                          className="h-2.5 w-2.5 rounded-full"
                          style={{ backgroundColor: colorOf(tx.category.color) }}
                        />
                        {tx.category.name}
                      </span>
                    ) : null}
                    <span className={`shrink-0 text-sm font-semibold tabular-nums ${amountTone(tx)}`}>
                      {amountPrefix(tx)}
                      {formatCurrency(Math.abs(tx.amount), tx.currency)}
                    </span>
                  </li>
                );
              })}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
