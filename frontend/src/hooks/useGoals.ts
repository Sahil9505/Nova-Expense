import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { goalsApi } from '@/lib/api';
import type {
  AddGoalContributionPayload,
  CreateGoalPayload,
  Goal,
  GoalDetail,
  GoalSummary,
  UpdateGoalPayload,
} from '@/types';

const GOALS_KEY = ['goals'] as const;

/** Loads the authenticated user's goals (active and inactive). */
export function useGoals() {
  return useQuery<Goal[]>({
    queryKey: GOALS_KEY,
    queryFn: () => goalsApi.list(),
  });
}

const GOAL_SUMMARY_KEY = ['goals', 'summary'] as const;

/** Loads the rolled-up Goal overview for the dashboard widgets. */
export function useGoalSummary() {
  return useQuery<GoalSummary>({
    queryKey: GOAL_SUMMARY_KEY,
    queryFn: () => goalsApi.summary(),
  });
}

function useInvalidateGoals() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: GOALS_KEY });
    queryClient.invalidateQueries({ queryKey: GOAL_SUMMARY_KEY });
  };
}

export function useCreateGoal() {
  const invalidate = useInvalidateGoals();
  return useMutation({
    mutationFn: (payload: CreateGoalPayload) => goalsApi.create(payload),
    onSuccess: invalidate,
  });
}

export function useUpdateGoal() {
  const invalidate = useInvalidateGoals();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateGoalPayload }) =>
      goalsApi.update(id, payload),
    onSuccess: invalidate,
  });
}

export function useDeleteGoal() {
  const invalidate = useInvalidateGoals();
  return useMutation({
    mutationFn: (id: string) => goalsApi.remove(id),
    onSuccess: invalidate,
  });
}

/** Adds a contribution and returns the updated detail (goal + contribution history). */
export function useAddGoalContribution() {
  const invalidate = useInvalidateGoals();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AddGoalContributionPayload }) =>
      goalsApi.addContribution(id, payload),
    onSuccess: invalidate,
  });
}

export type { Goal, GoalDetail, GoalSummary };
