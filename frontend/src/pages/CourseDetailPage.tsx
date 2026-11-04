import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { fetchCourse } from '../api/courses'
import {
  createAssessment,
  deleteAssessment,
  listAssessments,
  updateAssessment,
  type AssessmentInput,
} from '../api/assessments'
import { fetchCourseSummary } from '../api/summary'
import { AssessmentForm } from '../components/AssessmentForm'
import { AssessmentStatusBadge } from '../components/AssessmentStatusBadge'
import { RemindersPanel } from '../components/RemindersPanel'
import { StatusBadge } from '../components/StatusBadge'
import {
  cardClass,
  dangerButtonClass,
  linkClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../components/ui'
import { errorMessage } from '../utils/errors'
import { formatDate, formatGrade, formatPercent } from '../utils/format'
import { courseHint } from '../utils/grades'
import { assessmentTypeLabel } from '../utils/labels'

const CONFLICT = 'Esta asignatura ya tiene un examen.'

export function CourseDetailPage() {
  const { courseId: courseIdParam } = useParams()
  const courseId = Number(courseIdParam)
  const validId = Number.isInteger(courseId) && courseId > 0

  const queryClient = useQueryClient()
  const course = useQuery({
    queryKey: ['course', courseId],
    queryFn: () => fetchCourse(courseId),
    enabled: validId,
  })
  const summary = useQuery({
    queryKey: ['course-summary', courseId],
    queryFn: () => fetchCourseSummary(courseId),
    enabled: validId,
  })
  const assessments = useQuery({
    queryKey: ['assessments', courseId],
    queryFn: () => listAssessments(courseId),
    enabled: validId,
  })
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [remindersOpenId, setRemindersOpenId] = useState<number | null>(null)

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['assessments', courseId] })
    void queryClient.invalidateQueries({ queryKey: ['course-summary', courseId] })
    void queryClient.invalidateQueries({ queryKey: ['term-summary'] })
    void queryClient.invalidateQueries({ queryKey: ['summary'] })
    void queryClient.invalidateQueries({ queryKey: ['upcoming-reminders'] })
  }

  const createMutation = useMutation({
    mutationFn: (data: AssessmentInput) => createAssessment(courseId, data),
    onSuccess: () => {
      refresh()
      setCreating(false)
    },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: AssessmentInput }) => updateAssessment(id, data),
    onSuccess: () => {
      refresh()
      setEditingId(null)
    },
  })
  const deleteMutation = useMutation({ mutationFn: deleteAssessment, onSuccess: refresh })

  const items = assessments.data ?? []
  const totalWeight = Math.round(items.reduce((sum, a) => sum + a.weight * 100, 0)) / 100

  function handleDelete(id: number, name: string) {
    const confirmed = window.confirm(
      `Se borrará la evaluación "${name}" junto con sus avisos. ¿Continuar?`,
    )
    if (confirmed) {
      deleteMutation.mutate(id)
    }
  }

  const notFound = !validId || (course.error instanceof ApiError && course.error.status === 404)
  if (notFound) {
    return (
      <div className="space-y-3">
        <p className="text-slate-700">No se encontró la asignatura.</p>
        <Link to="/terms" className={linkClass}>
          Volver a los periodos
        </Link>
      </div>
    )
  }

  const s = summary.data

  return (
    <div className="space-y-6">
      {course.data && (
        <Link to={`/terms/${course.data.termId}`} className={`text-sm ${linkClass}`}>
          ← Volver al periodo
        </Link>
      )}

      {course.isPending && <p className="text-slate-600">Cargando...</p>}
      {course.isError && <p className="text-red-700">No se pudo cargar la asignatura.</p>}

      {course.data && (
        <section className={cardClass}>
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold text-slate-800">{course.data.name}</h1>
            {s && <StatusBadge status={s.status} />}
          </div>
          <p className="text-sm text-slate-600">
            {[
              course.data.code,
              course.data.credits === null ? null : `${course.data.credits} créditos`,
              `Aprueba con ${formatGrade(course.data.passingGrade)}`,
            ]
              .filter((part) => part !== null)
              .join(' · ')}
          </p>

          {s && (
            <>
              <p className="mt-3 text-slate-700">{courseHint(s)}</p>
              <dl className="mt-4 grid gap-3 sm:grid-cols-4">
                <div>
                  <dt className="text-xs uppercase tracking-wide text-slate-500">Nota final</dt>
                  <dd className="text-2xl font-bold text-slate-800">{formatGrade(s.finalGrade)}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-wide text-slate-500">Promedio actual</dt>
                  <dd className="text-2xl font-bold text-slate-800">{formatGrade(s.currentAverage)}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-wide text-slate-500">Evaluado</dt>
                  <dd className="text-2xl font-bold text-slate-800">{formatPercent(s.gradedWeight)}%</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-wide text-slate-500">Necesitas</dt>
                  <dd className="text-2xl font-bold text-slate-800">{formatGrade(s.requiredAverage)}</dd>
                </div>
              </dl>
            </>
          )}
        </section>
      )}

      {assessments.data && totalWeight !== 100 && (
        <p role="status" className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {items.length === 0
            ? 'Todavía no agregas evaluaciones. '
            : `Las ponderaciones suman ${formatPercent(totalWeight)}%. `}
          {totalWeight < 100
            ? `Faltan ${formatPercent(100 - totalWeight)}% por registrar para que el cálculo sea completo.`
            : ''}
        </p>
      )}

      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-800">Evaluaciones</h2>
        {!creating && (
          <button
            type="button"
            onClick={() => {
              createMutation.reset()
              setCreating(true)
            }}
            className={primaryButtonClass}
          >
            Nueva evaluación
          </button>
        )}
      </div>

      {creating && (
        <section className={cardClass}>
          <h3 className="mb-3 text-lg font-medium text-slate-800">Nueva evaluación</h3>
          <AssessmentForm
            otherAssessments={items}
            submitLabel="Crear evaluación"
            isPending={createMutation.isPending}
            error={errorMessage(createMutation.error, CONFLICT)}
            onSubmit={(data) => createMutation.mutate(data)}
            onCancel={() => setCreating(false)}
          />
        </section>
      )}

      {assessments.isPending && <p className="text-slate-600">Cargando evaluaciones...</p>}
      {assessments.isError && <p className="text-red-700">No se pudieron cargar las evaluaciones.</p>}
      {deleteMutation.isError && (
        <p className="text-red-700">No se pudo borrar la evaluación. Inténtalo de nuevo.</p>
      )}

      <ul className="space-y-4">
        {items.map((assessment) => (
          <li key={assessment.id} className={cardClass}>
            {editingId === assessment.id ? (
              <AssessmentForm
                initial={assessment}
                otherAssessments={items.filter((a) => a.id !== assessment.id)}
                submitLabel="Guardar cambios"
                isPending={updateMutation.isPending}
                error={errorMessage(updateMutation.error, CONFLICT)}
                onSubmit={(data) => updateMutation.mutate({ id: assessment.id, data })}
                onCancel={() => setEditingId(null)}
              />
            ) : (
              <>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-lg font-medium text-slate-800">{assessment.name}</h3>
                      <span className="rounded-full bg-indigo-50 px-2.5 py-0.5 text-xs font-medium text-indigo-700">
                        {assessmentTypeLabel[assessment.type]}
                      </span>
                      <AssessmentStatusBadge status={assessment.status} />
                    </div>
                    <p className="text-sm text-slate-600">
                      {formatPercent(assessment.weight)}% de la nota ·{' '}
                      {assessment.dueDate ? formatDate(assessment.dueDate) : 'Sin fecha'}
                      {assessment.grade !== null && ` · Nota ${formatGrade(assessment.grade)}`}
                    </p>
                    {assessment.topics && (
                      <p className="whitespace-pre-line text-sm text-slate-700">
                        <span className="font-medium">Contenidos: </span>
                        {assessment.topics}
                      </p>
                    )}
                    {assessment.comment && (
                      <p className="whitespace-pre-line text-sm text-slate-700">
                        <span className="font-medium">Comentario: </span>
                        {assessment.comment}
                      </p>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() =>
                        setRemindersOpenId(remindersOpenId === assessment.id ? null : assessment.id)
                      }
                      className={secondaryButtonClass}
                    >
                      Avisos
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        updateMutation.reset()
                        setEditingId(assessment.id)
                      }}
                      className={secondaryButtonClass}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      disabled={deleteMutation.isPending}
                      onClick={() => handleDelete(assessment.id, assessment.name)}
                      className={dangerButtonClass}
                    >
                      Eliminar
                    </button>
                  </div>
                </div>
                {remindersOpenId === assessment.id && <RemindersPanel assessment={assessment} />}
              </>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
