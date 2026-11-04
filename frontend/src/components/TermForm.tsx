import { useId, useState, type FormEvent } from 'react'
import type { Term, TermInput } from '../api/terms'
import { errorBoxClass, inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from './ui'

type Props = {
  initial?: Term
  submitLabel: string
  isPending: boolean
  error: string | null
  onSubmit: (data: TermInput) => void
  onCancel: () => void
}

export function TermForm({ initial, submitLabel, isPending, error, onSubmit, onCancel }: Props) {
  const id = useId()
  const [name, setName] = useState(initial?.name ?? '')
  const [startDate, setStartDate] = useState(initial?.startDate ?? '')
  const [endDate, setEndDate] = useState(initial?.endDate ?? '')
  const [localError, setLocalError] = useState<string | null>(null)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (endDate < startDate) {
      setLocalError('La fecha de término no puede ser anterior a la de inicio.')
      return
    }
    setLocalError(null)
    onSubmit({ name: name.trim(), startDate, endDate })
  }

  const message = localError ?? error

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div>
        <label htmlFor={`${id}-name`} className={labelClass}>
          Nombre del periodo
        </label>
        <input
          id={`${id}-name`}
          type="text"
          required
          maxLength={50}
          placeholder="Por ejemplo, 2027-1"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${id}-start`} className={labelClass}>
            Inicio
          </label>
          <input
            id={`${id}-start`}
            type="date"
            required
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${id}-end`} className={labelClass}>
            Término
          </label>
          <input
            id={`${id}-end`}
            type="date"
            required
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
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
