/** Convierte "2027-03-01" en "1 mar 2027" sin desfases de zona horaria. */
export function formatDate(iso: string): string {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('es-CL', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

/** Nota con coma decimal ("4,8"); "—" cuando todavía no hay valor. */
export function formatGrade(value: number | null | undefined, decimals = 1): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return value.toLocaleString('es-CL', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  })
}

/** Porcentaje sin decimales sobrantes: 35 -> "35", 33.33 -> "33,33". */
export function formatPercent(value: number): string {
  return value.toLocaleString('es-CL', { maximumFractionDigits: 2 })
}

/** Fecha de hoy en formato AAAA-MM-DD, según la zona horaria local. */
export function todayIso(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
