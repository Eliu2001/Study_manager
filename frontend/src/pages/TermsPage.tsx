import { useState } from 'react'
import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createTerm, deleteTerm, listTerms, updateTerm, type TermInput } from '../api/terms'
import { fetchOverallSummary } from '../api/summary'
import { TermForm } from '../components/TermForm'
import {
  cardClass,
  dangerButtonClass,
  linkClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../components/ui'
import { errorMessage } from '../utils/errors'
import { formatDate, formatGrade } from '../utils/format'

const CONFLICT = 'Ya tienes un periodo con ese nombre.'

export function TermsPage() {
  const queryClient = useQueryClient()
  const terms = useQuery({ queryKey: ['terms'], queryFn: listTerms })
  const overall = useQuery({ queryKey: ['summary'], queryFn: fetchOverallSummary })
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['terms'] })
    void queryClient.invalidateQueries({ queryKey: ['summary'] })
  }

  const createMutation = useMutation({
    mutationFn: createTerm,
    onSuccess: () => {
      refresh()
      setCreating(false)
    },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: TermInput }) => updateTerm(id, data),
    onSuccess: () => {
      refresh()
      setEditingId(null)
    },
  })
  const deleteMutation = useMutation({ mutationFn: deleteTerm, onSuccess: refresh })

  const averages = new Map<number, number | null>(
    (overall.data?.terms ?? []).map((t) => [t.termId, t.average] as [number, number | null]),
  )

  function handleDelete(id: number, name: string) {
    const confirmed = window.confirm(
      `Se borrará el periodo "${name}" junto con sus asignaturas, evaluaciones y avisos. ¿Continuar?`,
    )
    if (confirmed) {
      deleteMutation.mutate(id)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800">Periodos</h1>
        {!creating && (
          <button
            type="button"
            onClick={() => {
              createMutation.reset()
              setCreating(true)
            }}
            className={primaryButtonClass}
          >
            Nuevo periodo
          </button>
        )}
      </div>

      {creating && (
        <section className={cardClass}>
          <h2 className="mb-3 text-lg font-semibold text-slate-800">Nuevo periodo</h2>
          <TermForm
            submitLabel="Crear periodo"
            isPending={createMutation.isPending}
            error={errorMessage(createMutation.error, CONFLICT)}
            onSubmit={(data) => createMutation.mutate(data)}
            onCancel={() => setCreating(false)}
          />
        </section>
      )}

      {terms.isPending && <p className="text-slate-600">Cargando...</p>}
      {terms.isError && <p className="text-red-700">No se pudieron cargar los periodos.</p>}
      {deleteMutation.isError && (
        <p className="text-red-700">No se pudo borrar el periodo. Inténtalo de nuevo.</p>
      )}

      {terms.data && terms.data.length === 0 && !creating && (
        <p className="text-slate-600">
          Todavía no tienes periodos. Crea el primero para empezar a agregar asignaturas.
        </p>
      )}

      <ul className="space-y-4">
        {terms.data?.map((term) => (
          <li key={term.id} className={cardClass}>
            {editingId === term.id ? (
              <TermForm
                initial={term}
                submitLabel="Guardar cambios"
                isPending={updateMutation.isPending}
                error={errorMessage(updateMutation.error, CONFLICT)}
                onSubmit={(data) => updateMutation.mutate({ id: term.id, data })}
                onCancel={() => setEditingId(null)}
              />
            ) : (
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <Link to={`/terms/${term.id}`} className={`text-lg ${linkClass}`}>
                    {term.name}
                  </Link>
                  <p className="text-sm text-slate-600">
                    {formatDate(term.startDate)} – {formatDate(term.endDate)}
                  </p>
                  <p className="mt-1 text-sm text-slate-600">
                    Promedio del periodo: {formatGrade(averages.get(term.id))}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      updateMutation.reset()
                      setEditingId(term.id)
                    }}
                    className={secondaryButtonClass}
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    disabled={deleteMutation.isPending}
                    onClick={() => handleDelete(term.id, term.name)}
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
