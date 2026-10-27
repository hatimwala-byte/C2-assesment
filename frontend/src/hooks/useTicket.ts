import { useCallback, useEffect, useState } from 'react';

import { ApiError, NetworkError, ticketsApi } from '../api/client';
import type { Ticket } from '../api/types';

type LoadState = 'loading' | 'success' | 'error';

export function useTicket(ticketId: number | null) {
  const [loadState, setLoadState] = useState<LoadState>('loading');
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [error, setError] = useState<ApiError | NetworkError | null>(null);
  const [fetchToken, setFetchToken] = useState(0);

  const reload = useCallback(() => {
    setFetchToken((n) => n + 1);
  }, []);

  useEffect(() => {
    if (ticketId === null || !Number.isFinite(ticketId) || ticketId < 1) {
      setLoadState('error');
      setTicket(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoadState('loading');
    setError(null);

    ticketsApi
      .getById(ticketId)
      .then((data) => {
        if (!cancelled) {
          setTicket(data);
          setLoadState('success');
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setTicket(null);
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
  }, [ticketId, fetchToken]);

  return { loadState, ticket, setTicket, error, reload, isLoading: loadState === 'loading' };
}
