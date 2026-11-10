import { Link, NavLink, Outlet } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { fetchMe } from '../api/auth'
import { useAuth } from '../auth/AuthContext'
import { secondaryButtonClass } from './ui'

function navClassName({ isActive }: { isActive: boolean }): string {
  return (
    'flex min-h-11 items-center rounded-xl px-3 text-sm font-medium ' +
    (isActive ? 'bg-indigo-50 text-indigo-800' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900')
  )
}

export function AppLayout() {
  const { signOut } = useAuth()
  const me = useQuery({ queryKey: ['me'], queryFn: fetchMe })

  return (
    <div className="min-h-screen bg-slate-50">
      <a href="#main-content" className="sr-only z-20 rounded-lg bg-white p-3 text-indigo-800 focus:not-sr-only focus:fixed focus:left-4 focus:top-4">
        Ir al contenido principal
      </a>
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3 sm:px-6">
          <div className="flex min-w-0 flex-1 flex-wrap items-center gap-x-6 gap-y-2">
            <Link to="/" className="shrink-0 text-lg font-bold tracking-tight text-slate-900 no-underline">Study Manager</Link>
            <nav aria-label="Navegación principal" className="flex flex-wrap gap-1">
              <NavLink to="/" end className={navClassName}>
                Inicio
              </NavLink>
              <NavLink to="/terms" className={navClassName}>
                Periodos
              </NavLink>
              <NavLink to="/dates" className={navClassName}>
                Fechas
              </NavLink>
            </nav>
          </div>
          <div className="ml-auto flex items-center gap-3">
            {me.data && <span className="hidden max-w-40 truncate text-sm text-slate-600 sm:block">{me.data.name}</span>}
            <button type="button" onClick={signOut} className={secondaryButtonClass}>
              Cerrar sesión
            </button>
          </div>
        </div>
      </header>

      <main id="main-content" className="mx-auto w-full max-w-6xl p-4 sm:p-6 lg:p-8">
        <Outlet />
      </main>
    </div>
  )
}
