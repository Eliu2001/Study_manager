import { describe, expect, it } from 'vitest'
import { formatDate, formatGrade, formatPercent, todayIso } from './format'

describe('formatGrade', () => {
  it('usa coma decimal y un decimal por defecto', () => {
    expect(formatGrade(4.8)).toBe('4,8')
    expect(formatGrade(4)).toBe('4,0')
  })

  it('permite indicar la cantidad de decimales', () => {
    expect(formatGrade(4, 2)).toBe('4,00')
    expect(formatGrade(35, 0)).toBe('35')
  })

  it('muestra una raya cuando todavía no hay valor', () => {
    expect(formatGrade(null)).toBe('—')
    expect(formatGrade(undefined)).toBe('—')
  })
})

describe('formatPercent', () => {
  it('no agrega decimales sobrantes', () => {
    expect(formatPercent(35)).toBe('35')
    expect(formatPercent(33.333)).toBe('33,33')
  })
})

describe('formatDate', () => {
  it('no desplaza el día por la zona horaria', () => {
    const text = formatDate('2027-03-01')

    expect(text).toMatch(/^1\b/)
    expect(text).toContain('2027')
  })
})

describe('todayIso', () => {
  it('tiene formato AAAA-MM-DD', () => {
    expect(todayIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})
