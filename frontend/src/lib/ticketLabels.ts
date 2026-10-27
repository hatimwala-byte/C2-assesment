import type { TicketPriority, TicketStatus } from '../api/types';

const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  CANCELLED: 'Cancelled',
};

const PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
};

export const STATUS_FILTER_OPTIONS: { value: '' | TicketStatus; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'OPEN', label: STATUS_LABELS.OPEN },
  { value: 'IN_PROGRESS', label: STATUS_LABELS.IN_PROGRESS },
  { value: 'RESOLVED', label: STATUS_LABELS.RESOLVED },
  { value: 'CLOSED', label: STATUS_LABELS.CLOSED },
  { value: 'CANCELLED', label: STATUS_LABELS.CANCELLED },
];

export function statusLabel(status: TicketStatus): string {
  return STATUS_LABELS[status];
}

export function priorityLabel(priority: TicketPriority): string {
  return PRIORITY_LABELS[priority];
}
