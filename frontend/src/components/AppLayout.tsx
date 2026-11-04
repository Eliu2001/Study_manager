import { NavLink, Outlet } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { fetchMe } from '../api/auth'
import { useAuth } from '../auth/AuthContext'
import { secondaryButtonClass } from './ui'

function navClassName({ isActive }: { isActive: boolean }): string {
  return (
    'rounded-lg px-3 py-1.5 text-sm font-medium ' +
    (isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100')
  )
}

export function AppLayout() {
  const { signOut } = useAuth()
  const me = useQuery({ queryKey: ['me'], queryFn: fetchMe })

  return (
    <div className="min-h-screen bg-slate-100">
      <header className="bg-white shadow-sm">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <div className="flex items-center gap-4">
            <span className="text-lg font-bold text-slate-800">Study Manager</span>
            <nav className="flex gap-1">
              <NavLink to="/" end className={navClassName}>
                Inicio
              </NavLink>
              <NavLink to="/terms" className={navClassName}>
                Periodos
              </NavLink>
            </nav>
          </div>
          <div className="flex items-center gap-3">
            {me.data && <span className="text-sm text-slate-600">{me.data.name}</span>}
            <button type="button" onClick={signOut} className={secondaryButtonClass}>
              Cerrar sesión
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl p-4 sm:p-6">
        <Outlet />
      </main>
    </div>
  )
}
