import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../auth/AuthContext'

/** Deja pasar solo a quien inició sesión. */
export function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}

/** Para login y registro: si ya hay sesión, manda al inicio. */
export function GuestRoute() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Navigate to="/" replace /> : <Outlet />
}
