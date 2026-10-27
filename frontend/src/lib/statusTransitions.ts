import type { TicketStatus } from '../api/types';

export interface StatusTransitionAction {
  target: TicketStatus;
  label: string;
  variant?: 'primary' | 'danger';
  confirmMessage?: string;
}

const TRANSITIONS: Record<TicketStatus, StatusTransitionAction[]> = {
  OPEN: [
    { target: 'IN_PROGRESS', label: 'Start progress', variant: 'primary' },
    {
      target: 'CANCELLED',
      label: 'Cancel ticket',
      variant: 'danger',
      confirmMessage: 'Cancel this ticket? It will be marked as cancelled.',
    },
  ],
  IN_PROGRESS: [
    { target: 'RESOLVED', label: 'Mark resolved', variant: 'primary' },
    {
      target: 'CANCELLED',
      label: 'Cancel ticket',
      variant: 'danger',
      confirmMessage: 'Cancel this ticket? It will be marked as cancelled.',
    },
  ],
  RESOLVED: [{ target: 'CLOSED', label: 'Close ticket', variant: 'primary' }],
  CLOSED: [],
  CANCELLED: [],
};

export function allowedStatusActions(current: TicketStatus): StatusTransitionAction[] {
  return TRANSITIONS[current];
}

export function terminalStatusNote(current: TicketStatus): string | null {
  if (current === 'CLOSED') {
    return 'This ticket is closed.';
  }
  if (current === 'CANCELLED') {
    return 'This ticket is cancelled.';
  }
  return null;
}
