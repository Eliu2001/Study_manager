import { createBrowserRouter, Navigate } from 'react-router'
import { AppLayout } from './components/AppLayout'
import { GuestRoute, ProtectedRoute } from './components/ProtectedRoute'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { TermDetailPage } from './pages/TermDetailPage'
import { TermsPage } from './pages/TermsPage'

export const router = createBrowserRouter([
  {
    Component: GuestRoute,
    children: [
      { path: '/login', Component: LoginPage },
      { path: '/register', Component: RegisterPage },
    ],
  },
  {
    Component: ProtectedRoute,
    children: [
      {
        Component: AppLayout,
        children: [
          { path: '/', Component: HomePage },
          { path: '/terms', Component: TermsPage },
          { path: '/terms/:termId', Component: TermDetailPage },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])
