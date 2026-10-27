import { useState } from 'react';

import { ApiError, NetworkError, ticketsApi } from '../../api/client';
import type { Ticket, TicketStatus } from '../../api/types';
import { allowedStatusActions, terminalStatusNote } from '../../lib/statusTransitions';
import { statusLabel } from '../../lib/ticketLabels';

interface TicketStatusActionsProps {
  ticket: Ticket;
  disabled?: boolean;
  onTicketUpdated: (ticket: Ticket) => void;
  onReloadTicket: () => void;
}

export function TicketStatusActions({
  ticket,
  disabled,
  onTicketUpdated,
  onReloadTicket,
}: TicketStatusActionsProps) {
  const [updating, setUpdating] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [activeTarget, setActiveTarget] = useState<TicketStatus | null>(null);

  const actions = allowedStatusActions(ticket.status);
  const terminalNote = terminalStatusNote(ticket.status);

  const runTransition = async (action: ReturnType<typeof allowedStatusActions>[number]) => {
    if (action.confirmMessage && !window.confirm(action.confirmMessage)) {
      return;
    }

    setStatusError(null);
    setUpdating(true);
    setActiveTarget(action.target);

    try {
      const updated = await ticketsApi.updateStatus(ticket.id, action.target);
      onTicketUpdated(updated);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.isInvalidStateTransition()) {
          setStatusError(err.summaryMessage);
          onReloadTicket();
        } else if (err.isValidationError()) {
          setStatusError(err.summaryMessage || 'Invalid status change.');
          onReloadTicket();
        } else {
          setStatusError(err.summaryMessage || 'Could not update status.');
        }
      } else if (err instanceof NetworkError) {
        setStatusError('Something went wrong. Please try again.');
      } else {
        setStatusError('Something went wrong. Please try again.');
      }
    } finally {
      setUpdating(false);
      setActiveTarget(null);
    }
  };

  const sectionDisabled = disabled || updating;

  return (
    <section className="status-actions card-form" aria-labelledby="status-actions-heading">
      <div className="status-actions__header">
        <h2 id="status-actions-heading" className="section-title">Status actions</h2>
        <span className={`badge badge--status badge--${ticket.status.toLowerCase()}`}>
          {statusLabel(ticket.status)}
        </span>
      </div>

      {statusError && (
        <div className="status-actions__alert" role="alert">
          {statusError}
        </div>
      )}

      {updating && (
        <p className="status-actions__busy" aria-live="polite">
          Updating status…
        </p>
      )}

      {terminalNote ? (
        <p className="status-actions__note">{terminalNote}</p>
      ) : (
        <div className="status-actions__buttons">
          {actions.map((action) => (
            <button
              key={action.target}
              type="button"
              className={`btn ${
                action.variant === 'danger' ? 'btn--danger' : 'btn--primary'
              }`}
              disabled={sectionDisabled}
              onClick={() => runTransition(action)}
            >
              {updating && activeTarget === action.target ? 'Updating…' : action.label}
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
