import { useId, useState, type FormEvent } from 'react'
import type { AcademicDate, AcademicDateInput, AcademicDateType } from '../api/academicDates'
import { academicDateTypeLabel } from '../utils/labels'
import { errorBoxClass, inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from './ui'

type Props = {
  initial?: AcademicDate
  submitLabel: string
  isPending: boolean
  error: string | null
  onSubmit: (data: AcademicDateInput) => void
  onCancel: () => void
}

const TYPES: AcademicDateType[] = ['HOLIDAY', 'TERM_START', 'TERM_END', 'DEADLINE', 'OTHER']

export function AcademicDateForm({ initial, submitLabel, isPending, error, onSubmit, onCancel }: Props) {
  const id = useId()
  const [title, setTitle] = useState(initial?.title ?? '')
  const [eventDate, setEventDate] = useState(initial?.eventDate ?? '')
  const [type, setType] = useState<AcademicDateType>(initial?.type ?? 'OTHER')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    // Conserva la asignatura asociada si la fecha ya la tenía (por ejemplo, creada por la API).
    onSubmit({ title: title.trim(), eventDate, type, courseId: initial?.courseId ?? null })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div>
        <label htmlFor={`${id}-title`} className={labelClass}>
          Título
        </label>
        <input
          id={`${id}-title`}
          type="text"
          required
          maxLength={150}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className={inputClass}
        />
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${id}-date`} className={labelClass}>
            Fecha
          </label>
          <input
            id={`${id}-date`}
            type="date"
            required
            value={eventDate}
            onChange={(e) => setEventDate(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${id}-type`} className={labelClass}>
            Tipo
          </label>
          <select
            id={`${id}-type`}
            value={type}
            onChange={(e) => setType(e.target.value as AcademicDateType)}
            className={inputClass}
          >
            {TYPES.map((t) => (
              <option key={t} value={t}>
                {academicDateTypeLabel[t]}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <p role="alert" className={errorBoxClass}>
          {error}
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
