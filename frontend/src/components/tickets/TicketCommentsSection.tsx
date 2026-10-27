import { type FormEvent, useState } from 'react';

import { ApiError, NetworkError, commentsApi } from '../../api/client';
import { ErrorBanner } from '../common/ErrorBanner';
import { FormField } from '../common/FormField';
import { requestFailureMessage, validationBannerMessage } from '../../lib/formErrors';
import { formatDateTime } from '../../lib/formatDate';
import type { Comment } from '../../api/types';

interface TicketCommentsSectionProps {
  ticketId: number;
  comments: Comment[];
  isLoading: boolean;
  loadError: ApiError | NetworkError | null;
  loadMoreError: ApiError | NetworkError | null;
  hasMore: boolean;
  loadingMore: boolean;
  onReload: () => void;
  onLoadMore: () => void;
  onDismissLoadMoreError?: () => void;
  onCommentAdded: (comment: Comment) => void;
  disabled?: boolean;
}

export function TicketCommentsSection({
  ticketId,
  comments,
  isLoading,
  loadError,
  loadMoreError,
  hasMore,
  loadingMore,
  onReload,
  onLoadMore,
  onDismissLoadMoreError,
  onCommentAdded,
  disabled,
}: TicketCommentsSectionProps) {
  const [body, setBody] = useState('');
  const [posting, setPosting] = useState(false);
  const [clientError, setClientError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<ApiError | null>(null);
  const [requestError, setRequestError] = useState<ApiError | NetworkError | null>(null);

  const fieldErrors = [
    ...(clientError ? [clientError] : []),
    ...(apiError?.messagesForField('body') ?? []),
  ];

  const composeBanner =
    (apiError && validationBannerMessage(apiError)) ||
    (requestError && requestFailureMessage(requestError)) ||
    null;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setClientError(null);
    setApiError(null);
    setRequestError(null);

    const trimmed = body.trim();
    if (!trimmed) {
      setClientError('Comment cannot be empty.');
      return;
    }

    setPosting(true);
    try {
      const created = await commentsApi.create(ticketId, { body: trimmed });
      onCommentAdded(created);
      setBody('');
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.isValidationError()) {
          setApiError(err);
        } else {
          setRequestError(err);
        }
      } else if (err instanceof NetworkError) {
        setRequestError(err);
      } else {
        setRequestError(new NetworkError());
      }
    } finally {
      setPosting(false);
    }
  };

  return (
    <section className="comments-section card-form" aria-labelledby="comments-heading">
      <h2 id="comments-heading" className="section-title">Comments</h2>

      {isLoading && (
        <p className="comments-section__loading" aria-live="polite">Loading comments…</p>
      )}

      {!isLoading && loadError && (
        <ErrorBanner message={requestFailureMessage(loadError)} onRetry={onReload} />
      )}

      {!isLoading && !loadError && comments.length === 0 && (
        <p className="comments-section__empty">No comments yet.</p>
      )}

      {!isLoading && !loadError && comments.length > 0 && (
        <ul className="comment-list">
          {comments.map((comment) => (
            <li key={comment.id} className="comment-list__item">
              <time className="comment-list__time" dateTime={comment.createdAt}>
                {formatDateTime(comment.createdAt)}
              </time>
              <p className="comment-list__body">{comment.body}</p>
            </li>
          ))}
        </ul>
      )}

      {loadMoreError && (
        <ErrorBanner
          message={requestFailureMessage(loadMoreError)}
          onRetry={onLoadMore}
          onDismiss={onDismissLoadMoreError}
        />
      )}

      {hasMore && !loadError && (
        <div className="comments-section__more">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={loadingMore || disabled}
            onClick={onLoadMore}
          >
            {loadingMore ? 'Loading…' : 'Load more'}
          </button>
        </div>
      )}

      {!isLoading && (
        <form className="comment-compose" onSubmit={handleSubmit} noValidate>
          {composeBanner && <ErrorBanner message={composeBanner} />}
          <FormField label="Add a comment" htmlFor="comment-body" errors={fieldErrors}>
            <textarea
              id="comment-body"
              className="input input--textarea"
              rows={3}
              maxLength={2000}
              disabled={disabled || posting}
              placeholder="Write a comment…"
              value={body}
              onChange={(e) => {
                setBody(e.target.value);
                if (clientError) {
                  setClientError(null);
                }
              }}
              aria-invalid={fieldErrors.length > 0}
            />
          </FormField>
          <div className="comment-compose__actions">
            <button type="submit" className="btn btn--primary" disabled={disabled || posting}>
              {posting ? 'Posting…' : 'Post'}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
