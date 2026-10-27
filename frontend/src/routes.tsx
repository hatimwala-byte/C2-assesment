import { Navigate, createBrowserRouter } from 'react-router-dom';

import { AppShell } from './components/layout/AppShell';
import { CreateTicketPage } from './pages/tickets/CreateTicketPage';
import { TicketDetailPage } from './pages/tickets/TicketDetailPage';
import { TicketListPage } from './pages/tickets/TicketListPage';

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      {
        path: '/',
        element: <Navigate to="/tickets" replace />,
      },
      {
        path: '/tickets',
        element: <TicketListPage />,
      },
      {
        path: '/tickets/new',
        element: <CreateTicketPage />,
      },
      {
        path: '/tickets/:ticketId',
        element: <TicketDetailPage />,
      },
    ],
  },
]);
