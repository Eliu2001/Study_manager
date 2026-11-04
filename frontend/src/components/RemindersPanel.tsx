import { useId, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { Assessment } from '../api/assessments'
import { createReminder, deleteReminder, listReminders } from '../api/reminders'
import { errorMessage } from '../utils/errors'
import { errorBoxClass, inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from './ui'

function noticeLabel(days: number): string {
  if (days === 0) return 'El mismo día de la evaluación'
  if (days === 1) return '1 día antes'
  return `${days} días antes`
}

export function RemindersPanel({ assessment }: { assessment: Assessment }) {
  const id = useId()
  const queryClient = useQueryClient()
  const [days, setDays] = useState('1')
  const [localError, setLocalError] = useState<string | null>(null)

  const reminders = useQuery({
    queryKey: ['reminders', assessment.id],
    queryFn: () => listReminders(assessment.id),
  })

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['reminders', assessment.id] })
    void queryClient.invalidateQueries({ queryKey: ['upcoming-reminders'] })
  }

  const create = useMutation({
    mutationFn: (daysBefore: number) => createReminder(assessment.id, daysBefore),
    onSuccess: refresh,
  })
  const remove = useMutation({ mutationFn: deleteReminder, onSuccess: refresh })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = Number(days)
    if (!Number.isInteger(value) || value < 0 || value > 365) {
      setLocalError('Indica un número entero de días entre 0 y 365.')
      return
    }
    setLocalError(null)
    create.mutate(value)
  }

  const message = localError ?? errorMessage(create.error, 'Ya tienes un aviso con esa anticipación.')

  return (
    <div className="mt-4 space-y-3 rounded-lg bg-slate-50 p-4">
      <h4 className="text-sm font-medium text-slate-700">Avisos de esta evaluación</h4>

      {assessment.dueDate === null ? (
        <p className="text-sm text-slate-600">
          Agrega una fecha a la evaluación (con "Editar") para poder crear avisos.
        </p>
      ) : (
        <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-2">
          <div>
            <label htmlFor={`${id}-days`} className={labelClass}>
              Avisarme con (días de anticipación)
            </label>
            <input
              id={`${id}-days`}
              type="number"
              min={0}
              max={365}
              step={1}
              required
              value={days}
              onChange={(e) => setDays(e.target.value)}
              className={`${inputClass} w-40`}
            />
          </div>
          <button type="submit" disabled={create.isPending} className={primaryButtonClass}>
            {create.isPending ? 'Agregando...' : 'Agregar aviso'}
          </button>
        </form>
      )}

      {message && (
        <p role="alert" className={errorBoxClass}>
          {message}
        </p>
      )}

      {reminders.isPending && <p className="text-sm text-slate-600">Cargando...</p>}
      {reminders.isError && <p className="text-sm text-red-700">No se pudieron cargar los avisos.</p>}
      {reminders.data && reminders.data.length === 0 && (
        <p className="text-sm text-slate-600">Esta evaluación todavía no tiene avisos.</p>
      )}
      {reminders.data && reminders.data.length > 0 && (
        <ul className="divide-y divide-slate-200">
          {reminders.data.map((reminder) => (
            <li key={reminder.id} className="flex items-center justify-between gap-2 py-2">
              <span className="text-sm text-slate-700">
                {noticeLabel(reminder.daysBefore)}
                {reminder.sent && <span className="ml-2 text-xs text-slate-500">(ya visto)</span>}
              </span>
              <button
                type="button"
                disabled={remove.isPending}
                onClick={() => remove.mutate(reminder.id)}
                className={secondaryButtonClass}
              >
                Quitar
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
