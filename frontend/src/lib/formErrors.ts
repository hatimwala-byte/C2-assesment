import type { ApiError } from '../api/errors';
import { NetworkError } from '../api/errors';

export function requestFailureMessage(error: ApiError | NetworkError): string {
  if (error instanceof NetworkError) {
    return 'Something went wrong. Please try again.';
  }
  if (error.isServerError()) {
    return error.summaryMessage || 'Something went wrong. Please try again.';
  }
  return error.summaryMessage || 'Something went wrong. Please try again.';
}

export function validationBannerMessage(error: ApiError): string | null {
  if (!error.isValidationError()) {
    return null;
  }
  const unmapped = error.unmappedFieldErrors();
  if (unmapped.length === 0) {
    return error.summaryMessage;
  }
  const bullets = unmapped.map((e) => e.message).join(' ');
  return `${error.summaryMessage} ${bullets}`.trim();
}
