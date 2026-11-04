import { createBrowserRouter, Navigate } from 'react-router'
import { GuestRoute, ProtectedRoute } from './components/ProtectedRoute'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'

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
    children: [{ path: '/', Component: HomePage }],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])
