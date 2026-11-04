import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createCourse,
  deleteCourse,
  listCourses,
  updateCourse,
  type CourseInput,
} from '../api/courses'
import { ApiError } from '../api/client'
import { fetchTermSummary } from '../api/summary'
import { fetchTerm } from '../api/terms'
import { CourseForm } from '../components/CourseForm'
import { StatusBadge } from '../components/StatusBadge'
import {
  cardClass,
  dangerButtonClass,
  linkClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../components/ui'
import { errorMessage } from '../utils/errors'
import { formatDate, formatGrade } from '../utils/format'
import { courseHint } from '../utils/grades'

const CONFLICT = 'Ya tienes una asignatura con ese nombre en este periodo.'

export function TermDetailPage() {
  const { termId: termIdParam } = useParams()
  const termId = Number(termIdParam)
  const validId = Number.isInteger(termId) && termId > 0

  const queryClient = useQueryClient()
  const term = useQuery({ queryKey: ['term', termId], queryFn: () => fetchTerm(termId), enabled: validId })
  const summary = useQuery({
    queryKey: ['term-summary', termId],
    queryFn: () => fetchTermSummary(termId),
    enabled: validId,
  })
  const courses = useQuery({
    queryKey: ['courses', termId],
    queryFn: () => listCourses(termId),
    enabled: validId,
  })
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['courses', termId] })
    void queryClient.invalidateQueries({ queryKey: ['term-summary', termId] })
    void queryClient.invalidateQueries({ queryKey: ['summary'] })
  }

  const createMutation = useMutation({
    mutationFn: (data: CourseInput) => createCourse(termId, data),
    onSuccess: () => {
      refresh()
      setCreating(false)
    },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CourseInput }) => updateCourse(id, data),
    onSuccess: () => {
      refresh()
      setEditingId(null)
    },
  })
  const deleteMutation = useMutation({ mutationFn: deleteCourse, onSuccess: refresh })

  const summaryById = new Map(
    (summary.data?.courses ?? []).map((s) => [s.courseId, s] as const),
  )

  function handleDelete(id: number, name: string) {
    const confirmed = window.confirm(
      `Se borrará la asignatura "${name}" junto con sus evaluaciones y avisos. ¿Continuar?`,
    )
    if (confirmed) {
      deleteMutation.mutate(id)
    }
  }

  const notFound =
    !validId || (term.error instanceof ApiError && term.error.status === 404)
  if (notFound) {
    return (
      <div className="space-y-3">
        <p className="text-slate-700">No se encontró el periodo.</p>
        <Link to="/terms" className={linkClass}>
          Volver a los periodos
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <Link to="/terms" className={`text-sm ${linkClass}`}>
        ← Periodos
      </Link>

      {term.isPending && <p className="text-slate-600">Cargando...</p>}
      {term.isError && <p className="text-red-700">No se pudo cargar el periodo.</p>}

      {term.data && (
        <section className={cardClass}>
          <h1 className="text-2xl font-semibold text-slate-800">{term.data.name}</h1>
          <p className="text-sm text-slate-600">
            {formatDate(term.data.startDate)} – {formatDate(term.data.endDate)}
          </p>
          <p className="mt-3 text-sm text-slate-500">Promedio del periodo</p>
          <p className="text-3xl font-bold text-slate-800">
            {formatGrade(summary.data?.average)}
          </p>
        </section>
      )}

      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-800">Asignaturas</h2>
        {!creating && (
          <button
            type="button"
            onClick={() => {
              createMutation.reset()
              setCreating(true)
            }}
            className={primaryButtonClass}
          >
            Nueva asignatura
          </button>
        )}
      </div>

      {creating && (
        <section className={cardClass}>
          <h3 className="mb-3 text-lg font-medium text-slate-800">Nueva asignatura</h3>
          <CourseForm
            submitLabel="Crear asignatura"
            isPending={createMutation.isPending}
            error={errorMessage(createMutation.error, CONFLICT)}
            onSubmit={(data) => createMutation.mutate(data)}
            onCancel={() => setCreating(false)}
          />
        </section>
      )}

      {courses.isPending && <p className="text-slate-600">Cargando asignaturas...</p>}
      {courses.isError && <p className="text-red-700">No se pudieron cargar las asignaturas.</p>}
      {deleteMutation.isError && (
        <p className="text-red-700">No se pudo borrar la asignatura. Inténtalo de nuevo.</p>
      )}
      {courses.data && courses.data.length === 0 && !creating && (
        <p className="text-slate-600">Este periodo todavía no tiene asignaturas.</p>
      )}

      <ul className="space-y-4">
        {courses.data?.map((course) => {
          const courseSummary = summaryById.get(course.id)
          return (
            <li key={course.id} className={cardClass}>
              {editingId === course.id ? (
                <CourseForm
                  initial={course}
                  submitLabel="Guardar cambios"
                  isPending={updateMutation.isPending}
                  error={errorMessage(updateMutation.error, CONFLICT)}
                  onSubmit={(data) => updateMutation.mutate({ id: course.id, data })}
                  onCancel={() => setEditingId(null)}
                />
              ) : (
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-lg font-medium text-slate-800">{course.name}</h3>
                      {courseSummary && <StatusBadge status={courseSummary.status} />}
                    </div>
                    <p className="text-sm text-slate-600">
                      {[
                        course.code,
                        course.credits === null ? null : `${course.credits} créditos`,
                        `Aprueba con ${formatGrade(course.passingGrade)}`,
                      ]
                        .filter((part) => part !== null)
                        .join(' · ')}
                    </p>
                    {courseSummary && (
                      <p className="text-sm text-slate-700">{courseHint(courseSummary)}</p>
                    )}
                  </div>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        updateMutation.reset()
                        setEditingId(course.id)
                      }}
                      className={secondaryButtonClass}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      disabled={deleteMutation.isPending}
                      onClick={() => handleDelete(course.id, course.name)}
                      className={dangerButtonClass}
                    >
                      Eliminar
                    </button>
                  </div>
                </div>
              )}
            </li>
          )
        })}
      </ul>
    </div>
  )
}
