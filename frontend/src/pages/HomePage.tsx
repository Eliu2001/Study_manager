import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchMe } from '../api/auth'
import { dismissReminder, fetchUpcomingReminders } from '../api/reminders'
import { fetchOverallSummary } from '../api/summary'
import { cardClass, linkClass, secondaryButtonClass } from '../components/ui'
import { formatDate, formatGrade } from '../utils/format'

function whenLabel(daysLeft: number): string {
  if (daysLeft === 0) return 'hoy'
  if (daysLeft === 1) return 'mañana'
  return `en ${daysLeft} días`
}

export function HomePage() {
  const queryClient = useQueryClient()
  const me = useQuery({ queryKey: ['me'], queryFn: fetchMe })
  const overall = useQuery({ queryKey: ['summary'], queryFn: fetchOverallSummary })
  const upcoming = useQuery({ queryKey: ['upcoming-reminders'], queryFn: fetchUpcomingReminders })

  const dismiss = useMutation({
    mutationFn: dismissReminder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['upcoming-reminders'] }),
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-slate-800">
        {me.data ? `Hola, ${me.data.name}` : 'Hola'}
      </h1>

      <section className={cardClass}>
        <h2 className="text-sm font-medium uppercase tracking-wide text-slate-500">
          Promedio acumulado
        </h2>
        {overall.isPending && <p className="mt-2 text-slate-600">Cargando...</p>}
        {overall.isError && (
          <p className="mt-2 text-red-700">No se pudo cargar el promedio.</p>
        )}
        {overall.data && (
          <>
            <p className="mt-2 text-4xl font-bold text-slate-800">
              {formatGrade(overall.data.cumulativeAverage)}
            </p>
            <p className="mt-1 text-sm text-slate-500">
              {overall.data.cumulativeAverage === null
                ? 'Aparecerá cuando tengas asignaturas con todas sus notas.'
                : 'Considera solo las asignaturas con todas sus notas ingresadas.'}
            </p>

            {overall.data.terms.length > 0 && (
              <ul className="mt-4 divide-y divide-slate-100">
                {overall.data.terms.map((term) => (
                  <li key={term.termId} className="flex items-center justify-between py-2">
                    <Link to={`/terms/${term.termId}`} className={linkClass}>
                      {term.termName}
                    </Link>
                    <span className="text-slate-700">{formatGrade(term.average)}</span>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </section>

      <section className={cardClass}>
        <h2 className="text-sm font-medium uppercase tracking-wide text-slate-500">
          Avisos de hoy
        </h2>
        {upcoming.isPending && <p className="mt-2 text-slate-600">Cargando...</p>}
        {upcoming.isError && (
          <p className="mt-2 text-red-700">No se pudieron cargar los avisos.</p>
        )}
        {upcoming.data && upcoming.data.length === 0 && (
          <p className="mt-2 text-slate-600">No tienes avisos pendientes.</p>
        )}
        {upcoming.data && upcoming.data.length > 0 && (
          <ul className="mt-3 space-y-3">
            {upcoming.data.map((reminder) => (
              <li
                key={reminder.reminderId}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-amber-50 px-3 py-2"
              >
                <div>
                  <p className="font-medium text-slate-800">
                    {reminder.assessmentName} · {reminder.courseName}
                  </p>
                  <p className="text-sm text-slate-600">
                    {whenLabel(reminder.daysLeft)} ({formatDate(reminder.dueDate)})
                  </p>
                </div>
                <button
                  type="button"
                  disabled={dismiss.isPending}
                  onClick={() => dismiss.mutate(reminder.reminderId)}
                  className={secondaryButtonClass}
                >
                  Entendido
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
