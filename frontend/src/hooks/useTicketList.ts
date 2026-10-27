import { useCallback, useEffect, useState } from 'react';

import { ApiError, NetworkError, ticketsApi } from '../api/client';
import type { PagedResponse, Ticket } from '../api/types';

export const TICKET_LIST_PAGE_SIZE = 20;

export interface TicketListQuery {
  query: string;
  status: string;
  page: number;
}

type LoadState = 'loading' | 'success' | 'error';

export function useTicketList(params: TicketListQuery) {
  const [loadState, setLoadState] = useState<LoadState>('loading');
  const [pageData, setPageData] = useState<PagedResponse<Ticket> | null>(null);
  const [error, setError] = useState<ApiError | NetworkError | null>(null);
  const [fetchToken, setFetchToken] = useState(0);

  const retry = useCallback(() => {
    setFetchToken((n) => n + 1);
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoadState('loading');
    setError(null);

    ticketsApi
      .list({
        query: params.query.trim() || undefined,
        status: params.status || undefined,
        page: params.page,
        size: TICKET_LIST_PAGE_SIZE,
        sort: 'createdAt,desc',
      })
      .then((data) => {
        if (!cancelled) {
          setPageData(data);
          setLoadState('success');
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setPageData(null);
          setLoadState('error');
          if (err instanceof ApiError || err instanceof NetworkError) {
            setError(err);
          } else {
            setError(new NetworkError());
          }
        }
      });

    return () => {
      cancelled = true;
    };
  }, [params.query, params.status, params.page, fetchToken]);

  return {
    loadState,
    tickets: pageData?.content ?? [],
    pageData,
    error,
    retry,
    isLoading: loadState === 'loading',
  };
}
