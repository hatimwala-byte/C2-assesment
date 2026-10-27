import { type FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError, NetworkError, ticketsApi } from '../../api/client';
import { BackToTicketsLink } from '../../components/tickets/BackToTicketsLink';
import {
  DEFAULT_TICKET_FORM_VALUES,
  TicketFieldsForm,
  type TicketFormValues,
} from '../../components/tickets/TicketFieldsForm';
import { ErrorBanner } from '../../components/common/ErrorBanner';
import { requestFailureMessage, validationBannerMessage } from '../../lib/formErrors';

export function CreateTicketPage() {
  const navigate = useNavigate();
  const [values, setValues] = useState<TicketFormValues>(DEFAULT_TICKET_FORM_VALUES);
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState<ApiError | null>(null);
  const [requestError, setRequestError] = useState<ApiError | NetworkError | null>(null);

  const bannerMessage =
    (apiError && validationBannerMessage(apiError)) ||
    (requestError && requestFailureMessage(requestError)) ||
    null;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setApiError(null);
    setRequestError(null);
    setSubmitting(true);

    try {
      const created = await ticketsApi.create({
        title: values.title,
        description: values.description,
        priority: values.priority,
        assignee: values.assignee.trim() || undefined,
      });
      navigate(`/tickets/${created.id}`, {
        replace: true,
        state: { flash: { type: 'success', message: 'Ticket created successfully.' } },
      });
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
      setSubmitting(false);
    }
  };

  return (
    <div className="page">
      <div className="page-toolbar">
        <BackToTicketsLink />
      </div>
      <header className="page-header page-header--stacked">
        <h1 className="page-header__title">New ticket</h1>
      </header>

      {bannerMessage && <ErrorBanner message={bannerMessage} />}

      <form className="card-form" onSubmit={handleSubmit} noValidate>
        <TicketFieldsForm
          idPrefix="create"
          values={values}
          disabled={submitting}
          apiError={apiError}
          priorityControl="radio"
          onChange={setValues}
        />
        <div className="form-actions">
          <Link to="/tickets" className="btn btn--secondary">
            Cancel
          </Link>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create ticket'}
          </button>
        </div>
      </form>
    </div>
  );
}
