import { useId, useState, type FormEvent } from 'react'
import type { Course, CourseInput } from '../api/courses'
import { isValidCredits, MAX_CREDITS, MIN_CREDITS } from '../utils/credits'
import { errorBoxClass, inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from './ui'

type Props = {
  initial?: Course
  submitLabel: string
  isPending: boolean
  error: string | null
  onSubmit: (data: CourseInput) => void
  onCancel: () => void
}

export function CourseForm({ initial, submitLabel, isPending, error, onSubmit, onCancel }: Props) {
  const id = useId()
  const [name, setName] = useState(initial?.name ?? '')
  const [code, setCode] = useState(initial?.code ?? '')
  const [credits, setCredits] = useState(initial?.credits?.toString() ?? '')
  const [passingGrade, setPassingGrade] = useState((initial?.passingGrade ?? 4).toString())
  const [localError, setLocalError] = useState<string | null>(null)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const parsedCredits = credits.trim() === '' ? null : Number(credits)
    if (!isValidCredits(parsedCredits)) {
      setLocalError(`Los créditos deben ser un número entero entre ${MIN_CREDITS} y ${MAX_CREDITS}.`)
      return
    }
    const grade = Number(passingGrade)
    if (!Number.isFinite(grade) || grade < 1 || grade > 7) {
      setLocalError('La nota de aprobación debe estar entre 1,0 y 7,0.')
      return
    }
    setLocalError(null)
    onSubmit({
      name: name.trim(),
      code: code.trim() === '' ? null : code.trim(),
      credits: parsedCredits,
      passingGrade: Math.round(grade * 10) / 10,
    })
  }

  const message = localError ?? error

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div>
        <label htmlFor={`${id}-name`} className={labelClass}>
          Nombre de la asignatura
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
          <label htmlFor={`${id}-code`} className={labelClass}>
            Código (opcional)
          </label>
          <input
            id={`${id}-code`}
            type="text"
            maxLength={20}
            value={code}
            onChange={(e) => setCode(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${id}-credits`} className={labelClass}>
            Créditos (opcional)
          </label>
          <input
            id={`${id}-credits`}
            type="number"
            min={MIN_CREDITS}
            max={MAX_CREDITS}
            step={1}
            value={credits}
            onChange={(e) => setCredits(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${id}-passing`} className={labelClass}>
            Nota de aprobación
          </label>
          <input
            id={`${id}-passing`}
            type="number"
            required
            min={1}
            max={7}
            step={0.1}
            value={passingGrade}
            onChange={(e) => setPassingGrade(e.target.value)}
            className={inputClass}
          />
        </div>
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
