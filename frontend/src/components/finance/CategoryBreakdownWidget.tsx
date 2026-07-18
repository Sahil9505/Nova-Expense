import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { ChartCard } from '@/components/ui/chart-card';
import { ChartTooltip } from '@/components/ui/chart-tooltip';
import { Skeleton } from '@/components/ui/loading-state';
import { useAnalyticsCategories } from '@/hooks/useAnalytics';
import { colorOf } from '@/lib/finance';
import { formatCurrency } from '@/lib/utils';
import type { AnalyticsFilter } from '@/types';

interface CategoryBreakdownWidgetProps {
  filter?: AnalyticsFilter;
  title?: string;
  description?: string;
}

/**
 * Expense-by-category donut, powered by the Analytics domain's category endpoint.
 * Mirrors the dashboard "Spending by Category" widget so the two read identically.
 */
export function CategoryBreakdownWidget({
  filter = {},
  title = 'Spending by Category',
  description = 'Where your money went',
}: CategoryBreakdownWidgetProps) {
  const query = useAnalyticsCategories(filter);
  const data = (query.data?.expenses ?? []).map((item) => ({
    category: item.name,
    color: colorOf(item.color),
    amount: item.amount,
  }));
  const total = data.reduce((sum, entry) => sum + entry.amount, 0);

  return (
    <ChartCard title={title} description={description}>
      {query.isLoading ? (
        <Skeleton className="h-full w-full rounded-lg" />
      ) : data.length === 0 ? (
        <div className="flex h-full items-center justify-center">
          <p className="text-sm text-muted-foreground">No spending recorded in this period.</p>
        </div>
      ) : (
        <div className="flex h-full items-center gap-4">
          <div className="relative h-full w-1/2">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data}
                  dataKey="amount"
                  nameKey="category"
                  innerRadius="62%"
                  outerRadius="92%"
                  paddingAngle={2}
                  stroke="none"
                >
                  {data.map((entry) => (
                    <Cell key={entry.category} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  content={(props) => (
                    <ChartTooltip {...props} valueFormatter={(value) => formatCurrency(value)} />
                  )}
                />
              </PieChart>
            </ResponsiveContainer>
            <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-xs text-muted-foreground">Total</span>
              <span className="text-sm font-bold tabular-nums">{formatCurrency(total)}</span>
            </div>
          </div>
          <ul className="nova-scroll-thin flex min-h-0 max-h-full flex-1 flex-col gap-2 overflow-y-auto pr-1">
            {data.map((entry) => (
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
  );
}
