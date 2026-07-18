import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { ChartCard } from '@/components/ui/chart-card';
import { ChartLegend, ChartTooltip } from '@/components/ui/chart-tooltip';
import { Skeleton } from '@/components/ui/loading-state';
import { useAnalyticsCashFlow } from '@/hooks/useAnalytics';
import { useTheme } from '@/context/ThemeProvider';
import { formatCompact, formatCurrency } from '@/lib/utils';
import type { AnalyticsFilter } from '@/types';

interface CashFlowWidgetProps {
  filter?: AnalyticsFilter;
  title?: string;
  description?: string;
}

/**
 * Income-vs-expense trend, powered by the Analytics domain's cash-flow endpoint.
 * Reused on both the Analytics page and the dashboard, so the shape and styling stay
 * consistent with the existing dashboard charts.
 */
export function CashFlowWidget({
  filter = {},
  title = 'Cash Flow',
  description = 'Income vs. expenses over time',
}: CashFlowWidgetProps) {
  const { theme } = useTheme();
  const query = useAnalyticsCashFlow(filter);
  const axisColor = theme === 'dark' ? '#94A3B8' : '#475569';
  const gridColor = theme === 'dark' ? '#334155' : '#E2E8F0';

  return (
    <ChartCard
      title={title}
      description={description}
      action={
        <ChartLegend
          items={[
            { label: 'Income', color: '#10B981' },
            { label: 'Expenses', color: '#3B82F6' },
          ]}
        />
      }
    >
      {query.isLoading ? (
        <Skeleton className="h-full w-full rounded-lg" />
      ) : !query.data || query.data.points.length === 0 ? (
        <div className="flex h-full items-center justify-center">
          <p className="text-sm text-muted-foreground">No cash flow in this period yet.</p>
        </div>
      ) : (
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={query.data.points} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
            <defs>
              <linearGradient id="cfIncomeFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#10B981" stopOpacity={0.35} />
                <stop offset="100%" stopColor="#10B981" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="cfExpenseFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#3B82F6" stopOpacity={0.35} />
                <stop offset="100%" stopColor="#3B82F6" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke={gridColor} strokeOpacity={0.6} vertical={false} />
            <XAxis dataKey="label" stroke={axisColor} tickLine={false} axisLine={false} fontSize={12} />
            <YAxis
              stroke={axisColor}
              tickLine={false}
              axisLine={false}
              fontSize={12}
              width={48}
              tickFormatter={(value) => formatCompact(Number(value))}
            />
            <Tooltip
              cursor={{ stroke: gridColor, strokeWidth: 1, strokeDasharray: '4 4', fill: 'rgb(var(--primary) / 0.04)' }}
              content={(props) => (
                <ChartTooltip {...props} valueFormatter={(value) => formatCurrency(value)} />
              )}
            />
            <Area
              type="monotone"
              dataKey="income"
              name="Income"
              stroke="#10B981"
              strokeWidth={2}
              activeDot={{ r: 4, strokeWidth: 1.5, stroke: '#fff', fill: '#10B981' }}
              fill="url(#cfIncomeFill)"
            />
            <Area
              type="monotone"
              dataKey="expenses"
              name="Expenses"
              stroke="#3B82F6"
              strokeWidth={2}
              activeDot={{ r: 4, strokeWidth: 1.5, stroke: '#fff', fill: '#3B82F6' }}
              fill="url(#cfExpenseFill)"
            />
          </AreaChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
}
