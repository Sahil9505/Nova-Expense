import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { budgetsApi } from '@/lib/api';
import type { Budget, CreateBudgetPayload, UpdateBudgetPayload } from '@/types';

const BUDGETS_KEY = ['budgets'] as const;

/** Loads the authenticated user's budgets (active and inactive). */
export function useBudgets() {
  return useQuery<Budget[]>({
    queryKey: BUDGETS_KEY,
    queryFn: () => budgetsApi.list(),
  });
}

function useInvalidateBudgets() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: BUDGETS_KEY });
  };
}

export function useCreateBudget() {
  const invalidate = useInvalidateBudgets();
  return useMutation({
    mutationFn: (payload: CreateBudgetPayload) => budgetsApi.create(payload),
    onSuccess: invalidate,
  });
}

export function useUpdateBudget() {
  const invalidate = useInvalidateBudgets();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateBudgetPayload }) =>
      budgetsApi.update(id, payload),
    onSuccess: invalidate,
  });
}

export function useDeleteBudget() {
  const invalidate = useInvalidateBudgets();
  return useMutation({
    mutationFn: (id: string) => budgetsApi.remove(id),
    onSuccess: invalidate,
  });
}
