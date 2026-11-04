import { useId, useState, type FormEvent } from 'react'
import type { Assessment, AssessmentInput, AssessmentType } from '../api/assessments'
import { formatPercent } from '../utils/format'
import { assessmentTypeLabel } from '../utils/labels'
import { errorBoxClass, inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from './ui'

type Props = {
  initial?: Assessment
  /** Las demás evaluaciones de la asignatura, para validar ponderaciones y examen único. */
  otherAssessments: Assessment[]
  submitLabel: string
  isPending: boolean
  error: string | null
  onSubmit: (data: AssessmentInput) => void
  onCancel: () => void
}

export function AssessmentForm({
  initial,
  otherAssessments,
  submitLabel,
  isPending,
  error,
  onSubmit,
  onCancel,
}: Props) {
  const id = useId()
  const [name, setName] = useState(initial?.name ?? '')
  const [type, setType] = useState<AssessmentType>(initial?.type ?? 'SUMMATIVE')
  const [weight, setWeight] = useState(initial?.weight.toString() ?? '')
  const [dueDate, setDueDate] = useState(initial?.dueDate ?? '')
  const [status, setStatus] = useState<'PENDING' | 'SUBMITTED'>(
    initial?.status === 'SUBMITTED' ? 'SUBMITTED' : 'PENDING',
  )
  const [grade, setGrade] = useState(initial?.grade?.toString() ?? '')
  const [topics, setTopics] = useState(initial?.topics ?? '')
  const [comment, setComment] = useState(initial?.comment ?? '')
  const [localError, setLocalError] = useState<string | null>(null)

  const hasGrade = grade.trim() !== ''

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const weightNumber = Number(weight)
    if (!Number.isFinite(weightNumber) || weightNumber <= 0 || weightNumber > 100) {
      setLocalError('La ponderación debe estar entre 0,01% y 100%.')
      return
    }
    const weightCents = Math.round(weightNumber * 100)
    const othersCents = otherAssessments.reduce((sum, a) => sum + Math.round(a.weight * 100), 0)
    if (weightCents + othersCents > 10000) {
      setLocalError(
        `Las ponderaciones de la asignatura no pueden pasar de 100%. Las demás evaluaciones ya suman ${formatPercent(othersCents / 100)}%.`,
      )
      return
    }
    if (type === 'EXAM' && otherAssessments.some((a) => a.type === 'EXAM')) {
      setLocalError('Esta asignatura ya tiene un examen.')
      return
    }

    let gradeNumber: number | null = null
    if (hasGrade) {
      gradeNumber = Number(grade)
      if (!Number.isFinite(gradeNumber) || gradeNumber < 1 || gradeNumber > 7) {
        setLocalError('La nota debe estar entre 1,0 y 7,0.')
        return
      }
    }

    setLocalError(null)
    onSubmit({
      name: name.trim(),
      type,
      weight: weightCents / 100,
      dueDate: dueDate === '' ? null : dueDate,
      status: gradeNumber !== null ? 'GRADED' : status,
      grade: gradeNumber,
      topics: topics.trim() === '' ? null : topics.trim(),
      comment: comment.trim() === '' ? null : comment.trim(),
    })
  }

  const message = localError ?? error

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div>
        <label htmlFor={`${id}-name`} className={labelClass}>
          Nombre de la evaluación
        </label>
        <input
          id={`${id}-name`}
          type="text"
          required
          maxLength={120}
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div>
          <label htmlFor={`${id}-type`} className={labelClass}>
            Tipo
          </label>
          <select
            id={`${id}-type`}
            value={type}
            onChange={(e) => setType(e.target.value as AssessmentType)}
            className={inputClass}
          >
            <option value="SUMMATIVE">{assessmentTypeLabel.SUMMATIVE}</option>
            <option value="EXAM">{assessmentTypeLabel.EXAM}</option>
          </select>
        </div>
        <div>
          <label htmlFor={`${id}-weight`} className={labelClass}>
            Ponderación (%)
          </label>
          <input
            id={`${id}-weight`}
            type="number"
            required
            min={0.01}
            max={100}
            step={0.01}
            value={weight}
            onChange={(e) => setWeight(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${id}-due`} className={labelClass}>
            Fecha (opcional)
          </label>
          <input
            id={`${id}-due`}
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className={inputClass}
          />
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${id}-grade`} className={labelClass}>
            Nota (opcional)
          </label>
          <input
            id={`${id}-grade`}
            type="number"
            min={1}
            max={7}
            step={0.01}
            value={grade}
            onChange={(e) => setGrade(e.target.value)}
            className={inputClass}
          />
          <p className="mt-1 text-xs text-slate-500">
            Solo cuenta el primer decimal, sin redondear: 5,57 se guarda como 5,5.
          </p>
        </div>
        <div>
          <label htmlFor={`${id}-status`} className={labelClass}>
            Estado
          </label>
          <select
            id={`${id}-status`}
            value={hasGrade ? 'GRADED' : status}
            disabled={hasGrade}
            onChange={(e) => setStatus(e.target.value === 'SUBMITTED' ? 'SUBMITTED' : 'PENDING')}
            className={inputClass}
          >
            <option value="PENDING">Pendiente</option>
            <option value="SUBMITTED">Entregada</option>
            {hasGrade && <option value="GRADED">Calificada</option>}
          </select>
          {hasGrade && (
            <p className="mt-1 text-xs text-slate-500">Con nota, la evaluación queda como calificada.</p>
          )}
        </div>
      </div>

      <div>
        <label htmlFor={`${id}-topics`} className={labelClass}>
          Contenidos (opcional)
        </label>
        <textarea
          id={`${id}-topics`}
          rows={3}
          maxLength={5000}
          value={topics}
          onChange={(e) => setTopics(e.target.value)}
          className={inputClass}
        />
      </div>

      <div>
        <label htmlFor={`${id}-comment`} className={labelClass}>
          Comentario (opcional)
        </label>
        <textarea
          id={`${id}-comment`}
          rows={3}
          maxLength={5000}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          className={inputClass}
        />
      </div>

      {message && (
        <p role="alert" className={errorBoxClass}>
          {message}
        </p>
      )}

      <div className="flex gap-2">
        <button type="submit" disabled={isPending} className={primaryButtonClass}>
          {isPending ? 'Guardando...' : submitLabel}
        </button>
        <button type="button" onClick={onCancel} className={secondaryButtonClass}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
