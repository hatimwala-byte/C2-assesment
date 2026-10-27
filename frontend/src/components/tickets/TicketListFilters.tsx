import type { TicketStatus } from '../../api/types';
import { STATUS_FILTER_OPTIONS } from '../../lib/ticketLabels';

interface TicketListFiltersProps {
  searchValue: string;
  statusValue: string;
  disabled?: boolean;
  onSearchChange: (value: string) => void;
  onStatusChange: (status: '' | TicketStatus) => void;
}

export function TicketListFilters({
  searchValue,
  statusValue,
  disabled,
  onSearchChange,
  onStatusChange,
}: TicketListFiltersProps) {
  return (
    <div className="ticket-filters">
      <label className="field field--grow">
        <span className="field__label">Search</span>
        <input
          type="search"
          className="input"
          placeholder="Search by title or description…"
          value={searchValue}
          disabled={disabled}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </label>
      <label className="field">
        <span className="field__label">Status</span>
        <select
          className="select"
          value={statusValue}
          disabled={disabled}
          onChange={(e) => onStatusChange(e.target.value as '' | TicketStatus)}
        >
          {STATUS_FILTER_OPTIONS.map((option) => (
            <option key={option.label} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
