interface ErrorBannerProps {
  message: string;
  onRetry?: () => void;
  onDismiss?: () => void;
}

export function ErrorBanner({ message, onRetry, onDismiss }: ErrorBannerProps) {
  return (
    <div className="banner banner--error" role="alert">
      <p className="banner__message">{message}</p>
      <div className="banner__actions">
        {onRetry && (
          <button type="button" className="btn btn--secondary" onClick={onRetry}>
            Retry
          </button>
        )}
        {onDismiss && (
          <button type="button" className="btn btn--ghost" onClick={onDismiss} aria-label="Dismiss">
            Dismiss
          </button>
        )}
      </div>
    </div>
  );
}
