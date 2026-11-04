import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createAcademicDate,
  deleteAcademicDate,
  listAcademicDates,
  updateAcademicDate,
  type AcademicDateInput,
} from '../api/academicDates'
import { AcademicDateForm } from '../components/AcademicDateForm'
import {
  cardClass,
  dangerButtonClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../components/ui'
import { errorMessage } from '../utils/errors'
import { formatDate, todayIso } from '../utils/format'
import { academicDateTypeLabel } from '../utils/labels'

export function AcademicDatesPage() {
  const queryClient = useQueryClient()
  const [showPast, setShowPast] = useState(false)
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)

  const from = showPast ? undefined : todayIso()
  const dates = useQuery({
    queryKey: ['academic-dates', from ?? 'all'],
    queryFn: () => listAcademicDates(from),
  })

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['academic-dates'] })
  }

  const createMutation = useMutation({
    mutationFn: createAcademicDate,
    onSuccess: () => {
      refresh()
      setCreating(false)
    },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: AcademicDateInput }) => updateAcademicDate(id, data),
    onSuccess: () => {
      refresh()
      setEditingId(null)
    },
  })
  const deleteMutation = useMutation({ mutationFn: deleteAcademicDate, onSuccess: refresh })

  function handleDelete(id: number, title: string) {
    if (window.confirm(`Se borrará la fecha "${title}". ¿Continuar?`)) {
      deleteMutation.mutate(id)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800">Fechas académicas</h1>
        {!creating && (
          <button
            type="button"
            onClick={() => {
              createMutation.reset()
              setCreating(true)
            }}
            className={primaryButtonClass}
          >
            Nueva fecha
          </button>
        )}
      </div>

      <p className="text-sm text-slate-600">
        Feriados, inicio y término de semestre, entregas y otras fechas importantes. Las fechas de tus
        evaluaciones se ven dentro de cada asignatura.
      </p>

      <label className="flex items-center gap-2 text-sm text-slate-700">
        <input
          type="checkbox"
          checked={showPast}
          onChange={() => setShowPast(!showPast)}
        />
        Mostrar también las fechas pasadas
      </label>

      {creating && (
        <section className={cardClass}>
          <h2 className="mb-3 text-lg font-medium text-slate-800">Nueva fecha</h2>
          <AcademicDateForm
            submitLabel="Crear fecha"
            isPending={createMutation.isPending}
            error={errorMessage(createMutation.error, 'Ya existe una fecha con esos datos.')}
            onSubmit={(data) => createMutation.mutate(data)}
            onCancel={() => setCreating(false)}
          />
        </section>
      )}

      {dates.isPending && <p className="text-slate-600">Cargando...</p>}
      {dates.isError && <p className="text-red-700">No se pudieron cargar las fechas.</p>}
      {deleteMutation.isError && (
        <p className="text-red-700">No se pudo borrar la fecha. Inténtalo de nuevo.</p>
      )}
      {dates.data && dates.data.length === 0 && !creating && (
        <p className="text-slate-600">
          {showPast ? 'Todavía no tienes fechas.' : 'No tienes fechas próximas.'}
        </p>
      )}

      <ul className="space-y-4">
        {dates.data?.map((date) => (
          <li key={date.id} className={cardClass}>
            {editingId === date.id ? (
              <AcademicDateForm
                initial={date}
                submitLabel="Guardar cambios"
                isPending={updateMutation.isPending}
                error={errorMessage(updateMutation.error, 'Ya existe una fecha con esos datos.')}
                onSubmit={(data) => updateMutation.mutate({ id: date.id, data })}
                onCancel={() => setEditingId(null)}
              />
            ) : (
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-lg font-medium text-slate-800">{date.title}</p>
                  <p className="text-sm text-slate-600">
                    {formatDate(date.eventDate)} · {academicDateTypeLabel[date.type]}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      updateMutation.reset()
                      setEditingId(date.id)
                    }}
                    className={secondaryButtonClass}
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    disabled={deleteMutation.isPending}
                    onClick={() => handleDelete(date.id, date.title)}
                    className={dangerButtonClass}
                  >
                    Eliminar
                  </button>
                </div>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
