interface TicketListPaginationProps {
  page: number;
  totalPages: number;
  disabled?: boolean;
  onPageChange: (page: number) => void;
}

export function TicketListPagination({
  page,
  totalPages,
  disabled,
  onPageChange,
}: TicketListPaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  const displayPage = page + 1;

  return (
    <nav className="pagination" aria-label="Ticket list pagination">
      <button
        type="button"
        className="btn btn--secondary"
        disabled={disabled || page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        Previous
      </button>
      <span className="pagination__label">
        Page {displayPage} of {totalPages}
      </span>
      <button
        type="button"
        className="btn btn--secondary"
        disabled={disabled || page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Next
      </button>
    </nav>
  );
}
