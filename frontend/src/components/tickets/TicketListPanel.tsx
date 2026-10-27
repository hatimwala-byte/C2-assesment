import { Link } from 'react-router-dom';

interface TicketListPanelProps {
  variant: 'loading' | 'empty';
  hasActiveFilters: boolean;
  onClearFilters?: () => void;
}

export function TicketListPanel({ variant, hasActiveFilters, onClearFilters }: TicketListPanelProps) {
  if (variant === 'loading') {
    return (
      <div className="panel panel--centered" aria-live="polite" aria-busy="true">
        <p>Loading tickets…</p>
      </div>
    );
  }

  if (hasActiveFilters) {
    return (
      <div className="panel panel--centered">
        <p>No tickets match your search.</p>
        {onClearFilters && (
          <button type="button" className="btn btn--secondary" onClick={onClearFilters}>
            Clear filters
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="panel panel--centered">
      <p>No tickets yet.</p>
      <Link to="/tickets/new" className="btn btn--primary">
        Create your first ticket
      </Link>
    </div>
  );
}
