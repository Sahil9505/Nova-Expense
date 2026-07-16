/** Health payload returned by GET /api/health. */
export interface HealthStatus {
  status: 'UP' | 'DOWN';
  service: string;
  timestamp: string;
}

export type TrendDirection = 'up' | 'down' | 'flat';

export interface StatMetric {
  id: string;
  label: string;
  value: number;
  format: 'currency' | 'percent' | 'number';
  currency?: string;
  trend: number;
  trendDirection: TrendDirection;
  caption: string;
}

export interface SpendingPoint {
  month: string;
  income: number;
  expenses: number;
}

export interface CategoryBreakdown {
  category: string;
  amount: number;
  color: string;
}

export interface TransactionItem {
  id: string;
  description: string;
  category: string;
  amount: number;
  date: string;
  type: 'income' | 'expense';
}
