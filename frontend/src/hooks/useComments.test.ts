import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { NetworkError, commentsApi } from '../api/client';
import type { Comment } from '../api/types';

import { useComments } from './useComments';

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client');
  return {
    ...actual,
    commentsApi: {
      list: vi.fn(),
    },
  };
});

const listMock = vi.mocked(commentsApi.list);

const page0Comment: Comment = {
  id: 1,
  ticketId: 42,
  body: 'First page',
  createdAt: '2026-10-26T10:00:00.000Z',
};

describe('useComments', () => {
  beforeEach(() => {
    listMock.mockReset();
    listMock.mockResolvedValueOnce({
      content: [page0Comment],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 2,
      last: false,
    });
  });

  it('should_setLoadMoreError_when_loadMoreFails', async () => {
    const { result } = renderHook(() => useComments(42));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    listMock.mockRejectedValueOnce(new NetworkError());

    await act(async () => {
      await result.current.loadMore();
    });

    expect(result.current.loadMoreError).toBeInstanceOf(NetworkError);
    expect(result.current.comments).toHaveLength(1);
  });
});
