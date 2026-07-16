/**
 * Thin, typed HTTP client for the Nova API.
 *
 * The base URL is resolved from VITE_API_BASE_URL and defaults to the local
 * backend origin. In later phases this is where auth headers get attached.
 */

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

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  });

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

export const api = {
  get: <T>(path: string) => request<T>(path),
};
