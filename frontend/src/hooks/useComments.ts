import { useCallback, useEffect, useState } from 'react';

import { ApiError, NetworkError, commentsApi } from '../api/client';
import type { Comment } from '../api/types';

type LoadState = 'loading' | 'success' | 'error';

export function useComments(ticketId: number | null) {
  const [loadState, setLoadState] = useState<LoadState>('loading');
  const [comments, setComments] = useState<Comment[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loadError, setLoadError] = useState<ApiError | NetworkError | null>(null);
  const [loadMoreError, setLoadMoreError] = useState<ApiError | NetworkError | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [fetchToken, setFetchToken] = useState(0);

  const reload = useCallback(() => {
    setFetchToken((n) => n + 1);
  }, []);

  useEffect(() => {
    if (ticketId === null || ticketId < 1) {
      setLoadState('error');
      setComments([]);
      return;
    }

    let cancelled = false;
    setLoadState('loading');
    setLoadError(null);
    setLoadMoreError(null);
    setPage(0);

    commentsApi
      .list(ticketId, { page: 0, size: 20, sort: 'createdAt,asc' })
      .then((data) => {
        if (!cancelled) {
          setComments(data.content);
          setPage(data.page);
          setTotalPages(data.totalPages);
          setLoadState('success');
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setComments([]);
          setLoadState('error');
          if (err instanceof ApiError || err instanceof NetworkError) {
            setLoadError(err);
          } else {
            setLoadError(new NetworkError());
          }
        }
      });

    return () => {
      cancelled = true;
    };
  }, [ticketId, fetchToken]);

  const loadMore = useCallback(async () => {
    if (ticketId === null || loadingMore || page >= totalPages - 1) {
      return;
    }

    const nextPage = page + 1;
    setLoadingMore(true);
    setLoadMoreError(null);
    try {
      const data = await commentsApi.list(ticketId, {
        page: nextPage,
        size: 20,
        sort: 'createdAt,asc',
      });
      setComments((prev) => [...prev, ...data.content]);
      setPage(data.page);
      setTotalPages(data.totalPages);
    } catch (err: unknown) {
      if (err instanceof ApiError || err instanceof NetworkError) {
        setLoadMoreError(err);
      } else {
        setLoadMoreError(new NetworkError());
      }
    } finally {
      setLoadingMore(false);
    }
  }, [ticketId, loadingMore, page, totalPages]);

  const dismissLoadMoreError = useCallback(() => {
    setLoadMoreError(null);
  }, []);

  const appendComment = useCallback((comment: Comment) => {
    setComments((prev) => [...prev, comment]);
  }, []);

  const hasMore = totalPages > 0 && page < totalPages - 1;

  return {
    loadState,
    comments,
    loadError,
    loadMoreError,
    dismissLoadMoreError,
    reload,
    loadMore,
    loadingMore,
    hasMore,
    appendComment,
    isLoading: loadState === 'loading',
  };
}
