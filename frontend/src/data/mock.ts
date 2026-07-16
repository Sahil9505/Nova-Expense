import type {
  CategoryBreakdown,
  SpendingPoint,
  StatMetric,
  TransactionItem,
} from '@/types';

/**
 * Static sample data for the Phase 1 dashboard preview.
 *
 * This module intentionally decouples the UI from the API. When Phase 2 lands,
 * replace these imports with TanStack Query hooks backed by the Nova backend.
 */

export const statMetrics: StatMetric[] = [
  {
    id: 'balance',
    label: 'Total Balance',
    value: 48230.55,
    format: 'currency',
    currency: 'USD',
    trend: 4.2,
    trendDirection: 'up',
    caption: 'Across 3 accounts',
  },
  {
    id: 'spending',
    label: 'Monthly Spending',
    value: 3218.4,
    format: 'currency',
    currency: 'USD',
    trend: -2.8,
    trendDirection: 'down',
    caption: 'vs. last month',
  },
  {
    id: 'income',
    label: 'Monthly Income',
    value: 7850,
    format: 'currency',
    currency: 'USD',
    trend: 1.5,
    trendDirection: 'up',
    caption: 'vs. last month',
  },
  {
    id: 'savings',
    label: 'Savings Rate',
    value: 59,
    format: 'percent',
    trend: 6.1,
    trendDirection: 'up',
    caption: 'of monthly income',
  },
];

export const spendingTrend: SpendingPoint[] = [
  { month: 'Jan', income: 7400, expenses: 3800 },
  { month: 'Feb', income: 7200, expenses: 3450 },
  { month: 'Mar', income: 7850, expenses: 4120 },
  { month: 'Apr', income: 7600, expenses: 2980 },
  { month: 'May', income: 8100, expenses: 3610 },
  { month: 'Jun', income: 7850, expenses: 3218 },
];

export const categoryBreakdown: CategoryBreakdown[] = [
  { category: 'Housing', amount: 1450, color: '#3B82F6' },
  { category: 'Food', amount: 680, color: '#38BDF8' },
  { category: 'Transport', amount: 420, color: '#60A5FA' },
  { category: 'Shopping', amount: 360, color: '#10B981' },
  { category: 'Utilities', amount: 210, color: '#F59E0B' },
  { category: 'Other', amount: 98, color: '#94A3B8' },
];

export const recentTransactions: TransactionItem[] = [
  {
    id: 't1',
    description: 'Whole Foods Market',
    category: 'Food',
    amount: -86.42,
    date: '2026-07-15',
    type: 'expense',
  },
  {
    id: 't2',
    description: 'Monthly Salary',
    category: 'Income',
    amount: 7850,
    date: '2026-07-14',
    type: 'income',
  },
  {
    id: 't3',
    description: 'Shell Gas Station',
    category: 'Transport',
    amount: -52.1,
    date: '2026-07-13',
    type: 'expense',
  },
  {
    id: 't4',
    description: 'Netflix Subscription',
    category: 'Shopping',
    amount: -15.99,
    date: '2026-07-12',
    type: 'expense',
  },
  {
    id: 't5',
    description: 'Freelance Project',
    category: 'Income',
    amount: 1200,
    date: '2026-07-11',
    type: 'income',
  },
];

export const budgetProgress = [
  { category: 'Housing', spent: 1450, limit: 1600 },
  { category: 'Food', spent: 680, limit: 800 },
  { category: 'Transport', spent: 420, limit: 500 },
  { category: 'Shopping', spent: 360, limit: 400 },
];
