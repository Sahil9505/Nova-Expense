/**
 * Thin, typed HTTP client for the Nova API.
 *
 * The base URL is resolved from VITE_API_BASE_URL and defaults to the local
 * backend origin. Every request attaches the current access token (when present)
 * and, on a 401 from a protected route, attempts one silent refresh before
 * surfacing the error. Auth endpoints (login, register, refresh, logout) never
 * trigger a refresh loop.
 */
import type { ApiFieldError, AuthTokens } from '@/types';
import { tokenStorage } from './auth';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public details?: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** Extracts field-level errors from an ApiError's details, if present. */
export function getFieldErrors(error: unknown): ApiFieldError[] {
  if (error instanceof ApiError && error.details && typeof error.details === 'object') {
    const fields = (error.details as { fields?: ApiFieldError[] }).fields;
    if (Array.isArray(fields)) {
      return fields;
    }
  }
  return [];
}

// A single in-flight refresh is shared by every concurrent 401.
let refreshPromise: Promise<string | null> | null = null;

function authHeaders(): Record<string, string> {
  const token = tokenStorage.getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    tokenStorage.clear();
    return null;
  }
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) {
      tokenStorage.clear();
      return null;
    }
    const envelope = (await response.json()) as ApiEnvelope<AuthTokens>;
    tokenStorage.setTokens(envelope.data.accessToken, envelope.data.refreshToken);
    return envelope.data.accessToken;
  } catch {
    tokenStorage.clear();
    return null;
  }
}

async function unwrap<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    let details: unknown;
    try {
      const body = (await response.json()) as ApiEnvelope<unknown>;
      message = body.message ?? message;
      details = body.data;
    } catch {
      /* response had no JSON body */
    }
    throw new ApiError(response.status, message, details);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const envelope = (await response.json()) as ApiEnvelope<T>;
  return envelope.data;
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  const isAuthRoute =
    path.startsWith('/api/auth/login') ||
    path.startsWith('/api/auth/register') ||
    path.startsWith('/api/auth/refresh') ||
    path.startsWith('/api/auth/logout');

  const headers = {
    'Content-Type': 'application/json',
    ...authHeaders(),
    ...(init.headers ?? {}),
  };

  let response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });

  if (response.status === 401 && !isAuthRoute) {
    if (!refreshPromise) {
      refreshPromise = refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
    }
    const newToken = await refreshPromise;
    if (newToken) {
      const retryHeaders = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${newToken}`,
        ...(init.headers ?? {}),
      };
      response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers: retryHeaders });
    }
  }

  return unwrap<T>(response);
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};

// ---------------------------------------------------------------------------
// Finance API (Phase 3)
// ---------------------------------------------------------------------------

import type {
  Account,
  Budget,
  Category,
  CreateAccountPayload,
  CreateBudgetPayload,
  CreateCategoryPayload,
  CreateTransactionPayload,
  DashboardSummary,
  Transaction,
  TransactionQuery,
  UpdateAccountPayload,
  UpdateBudgetPayload,
  UpdateCategoryPayload,
  UpdateTransactionPayload,
} from '@/types';

export const accountsApi = {
  list: () => api.get<Account[]>('/api/accounts'),
  get: (id: string) => api.get<Account>(`/api/accounts/${id}`),
  create: (payload: CreateAccountPayload) => api.post<Account>('/api/accounts', payload),
  update: (id: string, payload: UpdateAccountPayload) =>
    api.patch<Account>(`/api/accounts/${id}`, payload),
  remove: (id: string) => api.delete<void>(`/api/accounts/${id}`),
};

export const categoriesApi = {
  list: () => api.get<Category[]>('/api/categories'),
  get: (id: string) => api.get<Category>(`/api/categories/${id}`),
  create: (payload: CreateCategoryPayload) => api.post<Category>('/api/categories', payload),
  update: (id: string, payload: UpdateCategoryPayload) =>
    api.patch<Category>(`/api/categories/${id}`, payload),
  remove: (id: string) => api.delete<void>(`/api/categories/${id}`),
};

export const transactionsApi = {
  list: (query: TransactionQuery = {}) => {
    const params = new URLSearchParams();
    if (query.type) params.set('type', query.type);
    if (query.accountId) params.set('accountId', query.accountId);
    if (query.categoryId) params.set('categoryId', query.categoryId);
    if (query.from) params.set('from', query.from);
    if (query.to) params.set('to', query.to);
    if (query.search) params.set('search', query.search);
    const qs = params.toString();
    return api.get<Transaction[]>(`/api/transactions${qs ? `?${qs}` : ''}`);
  },
  get: (id: string) => api.get<Transaction>(`/api/transactions/${id}`),
  create: (payload: CreateTransactionPayload) => api.post<Transaction>('/api/transactions', payload),
  update: (id: string, payload: UpdateTransactionPayload) =>
    api.patch<Transaction>(`/api/transactions/${id}`, payload),
  remove: (id: string) => api.delete<void>(`/api/transactions/${id}`),
};

export const dashboardApi = {
  summary: () => api.get<DashboardSummary>('/api/dashboard/summary'),
};

export const budgetsApi = {
  list: (active?: boolean) => {
    const params = new URLSearchParams();
    if (active !== undefined) params.set('active', String(active));
    const qs = params.toString();
    return api.get<Budget[]>(`/api/budgets${qs ? `?${qs}` : ''}`);
  },
  get: (id: string) => api.get<Budget>(`/api/budgets/${id}`),
  create: (payload: CreateBudgetPayload) => api.post<Budget>('/api/budgets', payload),
  update: (id: string, payload: UpdateBudgetPayload) =>
    api.patch<Budget>(`/api/budgets/${id}`, payload),
  remove: (id: string) => api.delete<void>(`/api/budgets/${id}`),
};
