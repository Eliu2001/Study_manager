import { useQuery } from '@tanstack/react-query'
import { fetchMe } from '../api/auth'
import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { signOut } = useAuth()
  const { data: user, isPending, isError } = useQuery({ queryKey: ['me'], queryFn: fetchMe })

  return (
    <main className="min-h-screen bg-slate-100">
      <header className="flex items-center justify-between bg-white px-6 py-4 shadow-sm">
        <h1 className="text-xl font-bold text-slate-800">Study Manager</h1>
        <button
          type="button"
          onClick={signOut}
          className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
        >
          Cerrar sesión
        </button>
      </header>

      <section className="mx-auto max-w-3xl p-6">
        {isPending && <p className="text-slate-600">Cargando...</p>}
        {isError && <p className="text-red-700">No se pudieron cargar tus datos.</p>}
        {user && (
          <>
            <h2 className="text-2xl font-semibold text-slate-800">Hola, {user.name}</h2>
            <p className="mt-2 text-slate-600">
              Tu sesión está activa. En los próximos pasos aquí aparecerán tus periodos y asignaturas.
            </p>
          </>
        )}
      </section>
    </main>
  )
}
