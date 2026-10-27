import {
  ApiError,
  NetworkError,
  parseApiErrorBody,
  toApiError,
} from './errors';
import type {
  Comment,
  CreateCommentPayload,
  CreateTicketPayload,
  ListCommentsParams,
  ListTicketsParams,
  PagedResponse,
  Ticket,
  TicketStatus,
  UpdateTicketPayload,
} from './types';

const API_VERSION_PREFIX = '/api/v1';

function apiRoot(): string {
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
  return base ? `${base}${API_VERSION_PREFIX}` : API_VERSION_PREFIX;
}

function buildUrl(path: string, query?: Record<string, string | number | undefined>): string {
  const url = new URL(`${apiRoot()}${path}`, window.location.origin);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

async function readErrorBody(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return null;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (init?.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  let response: Response;
  try {
    response = await fetch(path, { ...init, headers });
  } catch (cause) {
    throw new NetworkError(undefined, cause);
  }

  if (response.ok) {
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  const parsed = parseApiErrorBody(await readErrorBody(response));
  if (parsed) {
    throw toApiError(parsed);
  }

  throw new ApiError({
    timestamp: '',
    status: response.status,
    error: 'UNKNOWN_ERROR',
    message: response.statusText || 'Request failed.',
    errors: [],
  });
}

export const ticketsApi = {
  list(params: ListTicketsParams = {}): Promise<PagedResponse<Ticket>> {
    return request<PagedResponse<Ticket>>(
      buildUrl('/tickets', {
        query: params.query,
        status: params.status,
        priority: params.priority,
        assignee: params.assignee,
        page: params.page,
        size: params.size,
        sort: params.sort,
      }),
    );
  },

  getById(ticketId: number): Promise<Ticket> {
    return request<Ticket>(buildUrl(`/tickets/${ticketId}`));
  },

  create(payload: CreateTicketPayload): Promise<Ticket> {
    return request<Ticket>(buildUrl('/tickets'), {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  update(ticketId: number, payload: UpdateTicketPayload): Promise<Ticket> {
    return request<Ticket>(buildUrl(`/tickets/${ticketId}`), {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  updateStatus(ticketId: number, status: TicketStatus): Promise<Ticket> {
    return request<Ticket>(buildUrl(`/tickets/${ticketId}/status`), {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
  },
};

export const commentsApi = {
  list(ticketId: number, params: ListCommentsParams = {}): Promise<PagedResponse<Comment>> {
    return request<PagedResponse<Comment>>(
      buildUrl(`/tickets/${ticketId}/comments`, {
        page: params.page,
        size: params.size,
        sort: params.sort,
      }),
    );
  },

  create(ticketId: number, payload: CreateCommentPayload): Promise<Comment> {
    return request<Comment>(buildUrl(`/tickets/${ticketId}/comments`), {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
};

export { ApiError, NetworkError } from './errors';
export type { FieldError } from './errors';
