import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import { ApiError, NetworkError } from '../../api/client';
import type { TicketStatus } from '../../api/types';
import { ErrorBanner } from '../../components/common/ErrorBanner';
import { TicketListFilters } from '../../components/tickets/TicketListFilters';
import { TicketListPagination } from '../../components/tickets/TicketListPagination';
import { TicketListPanel } from '../../components/tickets/TicketListPanel';
import { TicketTable } from '../../components/tickets/TicketTable';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { useTicketList } from '../../hooks/useTicketList';

const SEARCH_DEBOUNCE_MS = 350;

function parsePage(raw: string | null): number {
  const n = Number(raw ?? '0');
  return Number.isFinite(n) && n >= 0 ? Math.floor(n) : 0;
}

function listErrorMessage(error: ApiError | NetworkError): string {
  if (error instanceof NetworkError) {
    return 'Something went wrong. Please try again.';
  }
  if (error.summaryMessage) {
    return error.summaryMessage;
  }
  return 'Something went wrong. Please try again.';
}

export function TicketListPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const urlQuery = searchParams.get('query') ?? '';
  const urlStatus = searchParams.get('status') ?? '';
  const urlPage = parsePage(searchParams.get('page'));

  const [searchInput, setSearchInput] = useState(urlQuery);
  const debouncedSearch = useDebouncedValue(searchInput, SEARCH_DEBOUNCE_MS);

  useEffect(() => {
    setSearchInput(urlQuery);
  }, [urlQuery]);

  useEffect(() => {
    const trimmed = debouncedSearch.trim();
    if (trimmed === urlQuery.trim()) {
      return;
    }
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (trimmed) {
          next.set('query', trimmed);
        } else {
          next.delete('query');
        }
        next.set('page', '0');
        return next;
      },
      { replace: true },
    );
  }, [debouncedSearch, urlQuery, setSearchParams]);

  const listQuery = useMemo(
    () => ({
      query: urlQuery,
      status: urlStatus,
      page: urlPage,
    }),
    [urlQuery, urlStatus, urlPage],
  );

  const { loadState, tickets, pageData, error, retry, isLoading } = useTicketList(listQuery);

  const hasActiveFilters = Boolean(urlQuery.trim() || urlStatus);

  const updateStatus = useCallback(
    (status: '' | TicketStatus) => {
      const next = new URLSearchParams(searchParams);
      if (status) {
        next.set('status', status);
      } else {
        next.delete('status');
      }
      next.set('page', '0');
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams],
  );

  const updatePage = useCallback(
    (page: number) => {
      const next = new URLSearchParams(searchParams);
      next.set('page', String(page));
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams],
  );

  const clearFilters = useCallback(() => {
    setSearchInput('');
    const next = new URLSearchParams();
    next.set('page', '0');
    setSearchParams(next, { replace: true });
  }, [setSearchParams]);

  const showTable = loadState === 'success' && tickets.length > 0;
  const showEmpty = loadState === 'success' && tickets.length === 0;
  const showError = loadState === 'error' && error;

  return (
    <div className="page">
      <header className="page-header">
        <h1 className="page-header__title">Support tickets</h1>
        <Link to="/tickets/new" className="btn btn--primary">
          New ticket
        </Link>
      </header>

      <TicketListFilters
        searchValue={searchInput}
        statusValue={urlStatus}
        disabled={isLoading}
        onSearchChange={setSearchInput}
        onStatusChange={updateStatus}
      />

      {showError && (
        <ErrorBanner message={listErrorMessage(error)} onRetry={retry} />
      )}

      {isLoading && <TicketListPanel variant="loading" hasActiveFilters={hasActiveFilters} />}

      {showEmpty && (
        <TicketListPanel
          variant="empty"
          hasActiveFilters={hasActiveFilters}
          onClearFilters={hasActiveFilters ? clearFilters : undefined}
        />
      )}

      {showTable && <TicketTable tickets={tickets} />}

      {showTable && pageData && (
        <TicketListPagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          disabled={isLoading}
          onPageChange={updatePage}
        />
      )}
    </div>
  );
}
