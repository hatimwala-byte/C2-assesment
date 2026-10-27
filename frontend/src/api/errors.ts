import type { ApiErrorBody, ApiFieldErrorBody } from './types';

export type ApiErrorCode =
  | 'BAD_REQUEST'
  | 'NOT_FOUND'
  | 'INVALID_STATE_TRANSITION'
  | 'INTERNAL_SERVER_ERROR'
  | (string & {});

export interface FieldError {
  field: string;
  rejectedValue?: unknown;
  message: string;
}

const KNOWN_FORM_FIELDS = new Set([
  'title',
  'description',
  'priority',
  'assignee',
  'body',
  'status',
]);

export class ApiError extends Error {
  readonly status: number;
  readonly code: ApiErrorCode;
  readonly summaryMessage: string;
  readonly fieldErrors: FieldError[];
  readonly timestamp?: string;

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.status = body.status;
    this.code = body.error as ApiErrorCode;
    this.summaryMessage = body.message;
    this.fieldErrors = (body.errors ?? []).map(normalizeFieldError);
    this.timestamp = body.timestamp;
  }

  isValidationError(): boolean {
    return this.status === 400 && this.code === 'BAD_REQUEST';
  }

  isNotFound(): boolean {
    return this.status === 404 && this.code === 'NOT_FOUND';
  }

  isInvalidStateTransition(): boolean {
    return this.status === 409 && this.code === 'INVALID_STATE_TRANSITION';
  }

  isServerError(): boolean {
    return this.status >= 500;
  }

  messagesForField(field: string): string[] {
    return this.fieldErrors.filter((e) => e.field === field).map((e) => e.message);
  }

  /** Field errors that are not mapped to a known form control (see ui-flow §3.1). */
  unmappedFieldErrors(): FieldError[] {
    return this.fieldErrors.filter((e) => !KNOWN_FORM_FIELDS.has(e.field));
  }
}

export class NetworkError extends Error {
  readonly cause?: unknown;

  constructor(message = 'Network request failed.', cause?: unknown) {
    super(message);
    this.name = 'NetworkError';
    this.cause = cause;
  }
}

export function parseApiErrorBody(data: unknown): ApiErrorBody | null {
  if (!data || typeof data !== 'object') {
    return null;
  }
  const record = data as Record<string, unknown>;
  if (
    typeof record.status !== 'number' ||
    typeof record.error !== 'string' ||
    typeof record.message !== 'string'
  ) {
    return null;
  }
  return {
    timestamp: typeof record.timestamp === 'string' ? record.timestamp : '',
    status: record.status,
    error: record.error,
    message: record.message,
    errors: Array.isArray(record.errors)
      ? record.errors
          .filter((item): item is ApiFieldErrorBody => isFieldErrorBody(item))
          .map(normalizeFieldError)
      : [],
  };
}

export function toApiError(body: ApiErrorBody): ApiError {
  return new ApiError(body);
}

/** Group validation messages by field name for inline form display. */
export function fieldErrorsByName(error: ApiError): Record<string, string[]> {
  const map: Record<string, string[]> = {};
  for (const fieldError of error.fieldErrors) {
    const list = map[fieldError.field] ?? [];
    list.push(fieldError.message);
    map[fieldError.field] = list;
  }
  return map;
}

function normalizeFieldError(raw: ApiFieldErrorBody): FieldError {
  return {
    field: raw.field,
    rejectedValue: raw.rejectedValue,
    message: raw.message,
  };
}

function isFieldErrorBody(value: unknown): value is ApiFieldErrorBody {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const record = value as Record<string, unknown>;
  return typeof record.field === 'string' && typeof record.message === 'string';
}
