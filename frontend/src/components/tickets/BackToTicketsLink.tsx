import { Link } from 'react-router-dom';

export function BackToTicketsLink() {
  return (
    <Link to="/tickets" className="back-link">
      ← Back to tickets
    </Link>
  );
}
