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
