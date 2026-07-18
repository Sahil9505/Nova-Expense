import { useMutation, useQuery } from '@tanstack/react-query';
import { analyticsApi } from '@/lib/api';
import type {
  AnalyticsExportFormat,
  AnalyticsFilter,
  BudgetAnalytics,
  CashFlowResponse,
  CategoryAnalysis,
  GoalAnalytics,
  SpendingOverview,
  AnalyticsOverview,
} from '@/types';

/** Full analytics snapshot for the applied filter (single source of truth for page + exports). */
export function useAnalyticsOverview(filter: AnalyticsFilter = {}) {
  return useQuery<AnalyticsOverview>({
    queryKey: ['analytics', 'overview', filter],
    queryFn: () => analyticsApi.overview(filter),
  });
}

export function useAnalyticsSpending(filter: AnalyticsFilter = {}) {
  return useQuery<SpendingOverview>({
    queryKey: ['analytics', 'spending', filter],
    queryFn: () => analyticsApi.spending(filter),
  });
}

export function useAnalyticsCashFlow(filter: AnalyticsFilter = {}) {
  return useQuery<CashFlowResponse>({
    queryKey: ['analytics', 'cashFlow', filter],
    queryFn: () => analyticsApi.cashFlow(filter),
  });
}

export function useAnalyticsCategories(filter: AnalyticsFilter = {}) {
  return useQuery<CategoryAnalysis>({
    queryKey: ['analytics', 'categories', filter],
    queryFn: () => analyticsApi.categories(filter),
  });
}

/** Budget health (current period) — independent of the date filter. */
export function useAnalyticsBudgets() {
  return useQuery<BudgetAnalytics>({
    queryKey: ['analytics', 'budgets'],
    queryFn: () => analyticsApi.budgets(),
  });
}

/** Goal progress (current) — independent of the date filter. */
export function useAnalyticsGoals() {
  return useQuery<GoalAnalytics>({
    queryKey: ['analytics', 'goals'],
    queryFn: () => analyticsApi.goals(),
  });
}

/** Triggers a CSV or PDF download built from the Analytics domain for the given filter. */
export function useAnalyticsExport() {
  return useMutation({
    mutationFn: ({ format, filter }: { format: AnalyticsExportFormat; filter?: AnalyticsFilter }) =>
      analyticsApi.exportReport(format, filter ?? {}),
  });
}
