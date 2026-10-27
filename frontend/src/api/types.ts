export type TicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED';

export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Ticket {
  id: number;
  title: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Comment {
  id: number;
  ticketId: number;
  body: string;
  createdAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface CreateTicketPayload {
  title: string;
  description: string;
  priority?: TicketPriority;
  assignee?: string;
}

export interface UpdateTicketPayload {
  title?: string;
  description?: string;
  priority?: TicketPriority;
  assignee?: string | null;
}

export interface UpdateTicketStatusPayload {
  status: TicketStatus;
}

export interface CreateCommentPayload {
  body: string;
}

export interface ListTicketsParams {
  query?: string;
  status?: string;
  priority?: string;
  assignee?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ListCommentsParams {
  page?: number;
  size?: number;
  sort?: string;
}

/** Wire format for API error responses (non-2xx). */
export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  errors?: ApiFieldErrorBody[];
}

export interface ApiFieldErrorBody {
  field: string;
  rejectedValue?: unknown;
  message: string;
}
