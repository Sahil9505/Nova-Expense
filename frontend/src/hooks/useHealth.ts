import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import type { HealthStatus } from '@/types';

/**
 * Polls the backend health endpoint. Designed to gracefully report a disconnected
 * state when no backend is reachable, so the dashboard never hard-crashes.
 */
export function useHealth() {
  return useQuery<HealthStatus>({
    queryKey: ['health'],
    queryFn: () => api.get<HealthStatus>('/api/health'),
    refetchInterval: 30_000,
    retry: false,
  });
}
