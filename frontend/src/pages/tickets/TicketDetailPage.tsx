import { type FormEvent, useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';

import { ApiError, NetworkError, ticketsApi } from '../../api/client';
import { ErrorBanner } from '../../components/common/ErrorBanner';
import { SuccessBanner } from '../../components/common/SuccessBanner';
import { BackToTicketsLink } from '../../components/tickets/BackToTicketsLink';
import { TicketCommentsSection } from '../../components/tickets/TicketCommentsSection';
import {
  TicketFieldsForm,
  ticketToFormValues,
  type TicketFormValues,
} from '../../components/tickets/TicketFieldsForm';
import { TicketStatusActions } from '../../components/tickets/TicketStatusActions';
import { useComments } from '../../hooks/useComments';
import { useTicket } from '../../hooks/useTicket';
import { formatDateTime } from '../../lib/formatDate';
import { requestFailureMessage, validationBannerMessage } from '../../lib/formErrors';
import { priorityLabel, statusLabel } from '../../lib/ticketLabels';

interface FlashMessage {
  type: 'success' | 'error';
  message: string;
}

function parseTicketId(raw: string | undefined): number | null {
  if (!raw) {
    return null;
  }
  const n = Number(raw);
  if (!Number.isInteger(n) || n < 1) {
    return null;
  }
  return n;
}

export function TicketDetailPage() {
  const { ticketId: ticketIdParam } = useParams();
  const ticketId = parseTicketId(ticketIdParam);
  const location = useLocation();

  const { loadState, ticket, setTicket, error: loadError, reload, isLoading } = useTicket(ticketId);
  const {
    comments,
    loadError: commentsLoadError,
    loadMoreError: commentsLoadMoreError,
    dismissLoadMoreError,
    reload: reloadComments,
    loadMore,
    loadingMore,
    hasMore,
    appendComment,
    isLoading: commentsLoading,
  } = useComments(ticketId);

  const [editing, setEditing] = useState(false);
  const [formValues, setFormValues] = useState<TicketFormValues | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveApiError, setSaveApiError] = useState<ApiError | null>(null);
  const [saveRequestError, setSaveRequestError] = useState<ApiError | NetworkError | null>(null);
  const [flash, setFlash] = useState<FlashMessage | null>(null);

  useEffect(() => {
    const state = location.state as { flash?: FlashMessage } | null;
    if (state?.flash) {
      setFlash(state.flash);
      window.history.replaceState({}, document.title);
    }
  }, [location.state]);

  useEffect(() => {
    if (ticket && !editing) {
      setFormValues(ticketToFormValues(ticket));
    }
  }, [ticket, editing]);

  const startEditing = () => {
    if (!ticket) {
      return;
    }
    setSaveApiError(null);
    setSaveRequestError(null);
    setFlash(null);
    setFormValues(ticketToFormValues(ticket));
    setEditing(true);
  };

  const cancelEditing = () => {
    if (ticket) {
      setFormValues(ticketToFormValues(ticket));
    }
    setSaveApiError(null);
    setSaveRequestError(null);
    setEditing(false);
  };

  const handleSave = async (event: FormEvent) => {
    event.preventDefault();
    if (!ticket || !formValues) {
      return;
    }

    setSaving(true);
    setSaveApiError(null);
    setSaveRequestError(null);
    setFlash(null);

    try {
      const updated = await ticketsApi.update(ticket.id, {
        title: formValues.title,
        description: formValues.description,
        priority: formValues.priority,
        assignee: formValues.assignee.trim() === '' ? null : formValues.assignee.trim(),
      });
      setTicket(updated);
      setFormValues(ticketToFormValues(updated));
      setEditing(false);
      setFlash({ type: 'success', message: 'Changes saved successfully.' });
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.isValidationError()) {
          setSaveApiError(err);
        } else {
          setSaveRequestError(err);
          setFlash({
            type: 'error',
            message: requestFailureMessage(err),
          });
        }
      } else if (err instanceof NetworkError) {
        setSaveRequestError(err);
        setFlash({ type: 'error', message: requestFailureMessage(err) });
      } else {
        setSaveRequestError(new NetworkError());
        setFlash({ type: 'error', message: requestFailureMessage(new NetworkError()) });
      }
    } finally {
      setSaving(false);
    }
  };

  const saveBannerMessage =
    (saveApiError && validationBannerMessage(saveApiError)) ||
    (saveRequestError && !flash && requestFailureMessage(saveRequestError)) ||
    null;

  if (ticketId === null) {
    return (
      <div className="page">
        <BackToTicketsLink />
        <div className="panel panel--centered">
          <p>Invalid ticket link.</p>
        </div>
      </div>
    );
  }

  if (isLoading && !ticket) {
    return (
      <div className="page">
        <BackToTicketsLink />
        <div className="panel panel--centered" aria-busy="true">
          <p>Loading ticket…</p>
        </div>
      </div>
    );
  }

  if (loadState === 'error' && loadError) {
    const notFound = loadError instanceof ApiError && loadError.isNotFound();
    return (
      <div className="page">
        <BackToTicketsLink />
        {notFound ? (
          <div className="panel panel--centered">
            <p>{loadError.summaryMessage || 'Ticket not found.'}</p>
            <BackToTicketsLink />
          </div>
        ) : (
          <ErrorBanner message={requestFailureMessage(loadError)} onRetry={reload} />
        )}
      </div>
    );
  }

  if (!ticket || !formValues) {
    return null;
  }

  return (
    <div className="page">
      <div className="page-toolbar page-toolbar--split">
        <BackToTicketsLink />
        <div className="page-toolbar__actions">
          {editing ? (
            <>
              <button type="button" className="btn btn--secondary" disabled={saving} onClick={cancelEditing}>
                Cancel
              </button>
              <button
                type="submit"
                form="ticket-edit-form"
                className="btn btn--primary"
                disabled={saving}
              >
                {saving ? 'Saving…' : 'Save changes'}
              </button>
            </>
          ) : (
            <button type="button" className="btn btn--secondary" onClick={startEditing}>
              Edit
            </button>
          )}
        </div>
      </div>

      <header className="page-header page-header--stacked">
        <h1 className="page-header__title">Ticket #{ticket.id}</h1>
      </header>

      {flash?.type === 'success' && (
        <SuccessBanner message={flash.message} onDismiss={() => setFlash(null)} />
      )}
      {flash?.type === 'error' && (
        <ErrorBanner message={flash.message} onDismiss={() => setFlash(null)} />
      )}

      {saveBannerMessage && editing && <ErrorBanner message={saveBannerMessage} />}

      {editing ? (
        <form id="ticket-edit-form" className="card-form" onSubmit={handleSave} noValidate>
          <TicketFieldsForm
            idPrefix="edit"
            values={formValues}
            disabled={saving}
            apiError={saveApiError}
            priorityControl="select"
            onChange={setFormValues}
          />
        </form>
      ) : (
        <section className="ticket-detail card-form">
          <dl className="detail-grid">
            <div className="detail-grid__row">
              <dt>Title</dt>
              <dd>{ticket.title}</dd>
            </div>
            <div className="detail-grid__row">
              <dt>Status</dt>
              <dd>
                <span className={`badge badge--status badge--${ticket.status.toLowerCase()}`}>
                  {statusLabel(ticket.status)}
                </span>
              </dd>
            </div>
            <div className="detail-grid__row">
              <dt>Priority</dt>
              <dd>{priorityLabel(ticket.priority)}</dd>
            </div>
            <div className="detail-grid__row">
              <dt>Assignee</dt>
              <dd>{ticket.assignee ?? '(none)'}</dd>
            </div>
            <div className="detail-grid__row">
              <dt>Created</dt>
              <dd>{formatDateTime(ticket.createdAt)}</dd>
            </div>
            <div className="detail-grid__row">
              <dt>Updated</dt>
              <dd>{formatDateTime(ticket.updatedAt)}</dd>
            </div>
          </dl>
          <div className="ticket-detail__description">
            <h2 className="section-title">Description</h2>
            <p className="ticket-detail__description-text">{ticket.description}</p>
          </div>
        </section>
      )}

      <TicketStatusActions
        ticket={ticket}
        disabled={saving}
        onTicketUpdated={(updated) => {
          setTicket(updated);
          if (!editing) {
            setFormValues(ticketToFormValues(updated));
          }
        }}
        onReloadTicket={reload}
      />

      <TicketCommentsSection
        ticketId={ticket.id}
        comments={comments}
        isLoading={commentsLoading}
        loadError={commentsLoadError}
        loadMoreError={commentsLoadMoreError}
        hasMore={hasMore}
        loadingMore={loadingMore}
        onReload={reloadComments}
        onLoadMore={loadMore}
        onDismissLoadMoreError={dismissLoadMoreError}
        onCommentAdded={appendComment}
        disabled={saving}
      />
    </div>
  );
}
