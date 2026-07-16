import { type CSSProperties } from 'react';
import { ArrowDownLeft, ArrowUpRight } from 'lucide-react';
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
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ChartCard } from '@/components/ui/chart-card';
import { StatCard } from '@/components/ui/stat-card';
import { useTheme } from '@/context/ThemeProvider';
import { formatCompact, formatCurrency } from '@/lib/utils';
import {
  budgetProgress,
  categoryBreakdown,
  recentTransactions,
  spendingTrend,
  statMetrics,
} from '@/data/mock';

export function Dashboard() {
  const { theme } = useTheme();
  const axisColor = theme === 'dark' ? '#94A3B8' : '#475569';
  const gridColor = theme === 'dark' ? '#334155' : '#E2E8F0';
  const tooltipStyle: CSSProperties = {
    backgroundColor: theme === 'dark' ? '#1E293B' : '#FFFFFF',
    border: `1px solid ${gridColor}`,
    borderRadius: '0.5rem',
    color: theme === 'dark' ? '#F8FAFC' : '#0F172A',
    fontSize: '12px',
  };
  const totalSpending = categoryBreakdown.reduce((sum, c) => sum + c.amount, 0);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Dashboard</h2>
          <p className="text-sm text-muted-foreground">
            A snapshot of your finances this month.
          </p>
        </div>
        <Badge variant="outline">July 2026</Badge>
      </div>

      <section aria-label="Key metrics" className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statMetrics.map((metric) => (
          <StatCard key={metric.id} metric={metric} />
        ))}
      </section>

      <section aria-label="Trends" className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ChartCard title="Cash Flow" description="Income vs. expenses over time">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={spendingTrend} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
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
              <XAxis dataKey="month" stroke={axisColor} tickLine={false} axisLine={false} fontSize={12} />
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
          <div className="flex h-full items-center gap-4">
            <div className="relative h-full w-1/2">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryBreakdown}
                    dataKey="amount"
                    nameKey="category"
                    innerRadius="62%"
                    outerRadius="92%"
                    paddingAngle={2}
                    stroke="none"
                  >
                    {categoryBreakdown.map((entry) => (
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
              {categoryBreakdown.map((entry) => (
                <li key={entry.category} className="flex items-center gap-2 text-sm">
                  <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: entry.color }} />
                  <span className="text-muted-foreground">{entry.category}</span>
                  <span className="ml-auto font-medium tabular-nums">{formatCurrency(entry.amount)}</span>
                </li>
              ))}
            </ul>
          </div>
        </ChartCard>
      </section>

      <section className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Budget Progress</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            {budgetProgress.map((item) => {
              const pct = Math.min(100, Math.round((item.spent / item.limit) * 100));
              const over = item.spent > item.limit;
              return (
                <div key={item.category}>
                  <div className="mb-1 flex items-center justify-between text-sm">
                    <span className="font-medium">{item.category}</span>
                    <span className="text-muted-foreground tabular-nums">
                      {formatCurrency(item.spent)} / {formatCurrency(item.limit)}
                    </span>
                  </div>
                  <div
                    className="h-2 w-full overflow-hidden rounded-full bg-surface-2"
                    role="progressbar"
                    aria-valuenow={pct}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-label={`${item.category} budget`}
                  >
                    <div
                      className={`h-full rounded-full ${over ? 'bg-danger' : 'bg-primary'}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Recent Transactions</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col">
            {recentTransactions.map((tx) => {
              const isIncome = tx.type === 'income';
              return (
                <div
                  key={tx.id}
                  className="flex items-center gap-3 border-b border-border py-3 last:border-0"
                >
                  <div
                    className={`flex h-9 w-9 items-center justify-center rounded-full ${
                      isIncome ? 'bg-success/15 text-success' : 'bg-danger/15 text-danger'
                    }`}
                  >
                    {isIncome ? (
                      <ArrowDownLeft className="h-4 w-4" aria-hidden="true" />
                    ) : (
                      <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{tx.description}</p>
                    <p className="text-xs text-muted-foreground">{tx.category}</p>
                  </div>
                  <span
                    className={`text-sm font-semibold tabular-nums ${
                      isIncome ? 'text-success' : 'text-foreground'
                    }`}
                  >
                    {isIncome ? '+' : '-'}
                    {formatCurrency(Math.abs(tx.amount))}
                  </span>
                </div>
              );
            })}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
