import type { ApiError } from '../../api/errors';
import type { TicketPriority } from '../../api/types';
import { FormField } from '../common/FormField';

export interface TicketFormValues {
  title: string;
  description: string;
  priority: TicketPriority;
  assignee: string;
}

export const DEFAULT_TICKET_FORM_VALUES: TicketFormValues = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  assignee: '',
};

interface TicketFieldsFormProps {
  idPrefix: string;
  values: TicketFormValues;
  disabled?: boolean;
  apiError?: ApiError | null;
  priorityControl?: 'radio' | 'select';
  onChange: (values: TicketFormValues) => void;
}

const PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH'];

const PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
};

export function TicketFieldsForm({
  idPrefix,
  values,
  disabled,
  apiError,
  priorityControl = 'radio',
  onChange,
}: TicketFieldsFormProps) {
  const fieldErrors = (field: string) => apiError?.messagesForField(field) ?? [];

  const set = (patch: Partial<TicketFormValues>) => {
    onChange({ ...values, ...patch });
  };

  return (
    <div className="ticket-form-fields">
      <FormField
        label="Title"
        htmlFor={`${idPrefix}-title`}
        required
        errors={fieldErrors('title')}
      >
        <input
          id={`${idPrefix}-title`}
          className="input"
          type="text"
          maxLength={200}
          disabled={disabled}
          value={values.title}
          onChange={(e) => set({ title: e.target.value })}
          aria-invalid={fieldErrors('title').length > 0}
          aria-describedby={fieldErrors('title').length ? `${idPrefix}-title-errors` : undefined}
        />
      </FormField>

      <FormField
        label="Description"
        htmlFor={`${idPrefix}-description`}
        required
        errors={fieldErrors('description')}
      >
        <textarea
          id={`${idPrefix}-description`}
          className="input input--textarea"
          rows={5}
          maxLength={5000}
          disabled={disabled}
          value={values.description}
          onChange={(e) => set({ description: e.target.value })}
          aria-invalid={fieldErrors('description').length > 0}
          aria-describedby={
            fieldErrors('description').length ? `${idPrefix}-description-errors` : undefined
          }
        />
      </FormField>

      <FormField label="Priority" htmlFor={`${idPrefix}-priority`} errors={fieldErrors('priority')}>
        {priorityControl === 'select' ? (
          <select
            id={`${idPrefix}-priority`}
            className="select"
            disabled={disabled}
            value={values.priority}
            onChange={(e) => set({ priority: e.target.value as TicketPriority })}
          >
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {PRIORITY_LABELS[p]}
              </option>
            ))}
          </select>
        ) : (
          <div className="priority-radios" id={`${idPrefix}-priority`}>
            {PRIORITIES.map((p) => (
              <label key={p} className="priority-radios__option">
                <input
                  type="radio"
                  name={`${idPrefix}-priority`}
                  value={p}
                  disabled={disabled}
                  checked={values.priority === p}
                  onChange={() => set({ priority: p })}
                />
                {PRIORITY_LABELS[p]}
              </label>
            ))}
          </div>
        )}
      </FormField>

      <FormField label="Assignee" htmlFor={`${idPrefix}-assignee`} errors={fieldErrors('assignee')}>
        <input
          id={`${idPrefix}-assignee`}
          className="input"
          type="text"
          maxLength={100}
          disabled={disabled}
          placeholder="Optional"
          value={values.assignee}
          onChange={(e) => set({ assignee: e.target.value })}
          aria-invalid={fieldErrors('assignee').length > 0}
          aria-describedby={fieldErrors('assignee').length ? `${idPrefix}-assignee-errors` : undefined}
        />
      </FormField>
    </div>
  );
}

export function ticketToFormValues(ticket: {
  title: string;
  description: string;
  priority: TicketPriority;
  assignee: string | null;
}): TicketFormValues {
  return {
    title: ticket.title,
    description: ticket.description,
    priority: ticket.priority,
    assignee: ticket.assignee ?? '',
  };
}
