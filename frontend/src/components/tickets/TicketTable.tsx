import { Link } from 'react-router-dom';

import type { Ticket } from '../../api/types';
import { formatDateTime } from '../../lib/formatDate';
import { priorityLabel, statusLabel } from '../../lib/ticketLabels';

interface TicketTableProps {
  tickets: Ticket[];
}

export function TicketTable({ tickets }: TicketTableProps) {
  return (
    <div className="table-wrap">
      <table className="ticket-table">
        <thead>
          <tr>
            <th scope="col">Title</th>
            <th scope="col">Status</th>
            <th scope="col">Priority</th>
            <th scope="col">Assignee</th>
            <th scope="col">Created</th>
          </tr>
        </thead>
        <tbody>
          {tickets.map((ticket) => (
            <tr key={ticket.id}>
              <td>
                <Link to={`/tickets/${ticket.id}`} className="ticket-table__title">
                  {ticket.title}
                </Link>
              </td>
              <td>
                <span className={`badge badge--status badge--${ticket.status.toLowerCase()}`}>
                  {statusLabel(ticket.status)}
                </span>
              </td>
              <td>{priorityLabel(ticket.priority)}</td>
              <td className="ticket-table__muted">
                {ticket.assignee ?? '(none)'}
              </td>
              <td className="ticket-table__muted">{formatDateTime(ticket.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
