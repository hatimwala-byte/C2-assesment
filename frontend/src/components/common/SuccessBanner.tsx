interface SuccessBannerProps {
  message: string;
  onDismiss?: () => void;
}

export function SuccessBanner({ message, onDismiss }: SuccessBannerProps) {
  return (
    <div className="banner banner--success" role="status">
      <p className="banner__message">{message}</p>
      {onDismiss && (
        <div className="banner__actions">
          <button type="button" className="btn btn--ghost" onClick={onDismiss} aria-label="Dismiss">
            Dismiss
          </button>
        </div>
      )}
    </div>
  );
}
