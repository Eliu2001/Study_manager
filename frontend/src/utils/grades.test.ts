import { describe, expect, it } from 'vitest'
import type { CourseStatus, CourseSummary } from '../api/summary'
import { courseHint, statusClass, statusLabel } from './grades'

function summary(overrides: Partial<CourseSummary>): CourseSummary {
  return {
    courseId: 1,
    courseName: 'Bases de Datos',
    credits: null,
    passingGrade: 4,
    gradedWeight: 0,
    currentPoints: 0,
    currentAverage: null,
    finalGrade: null,
    requiredAverage: null,
    status: 'IN_PROGRESS',
    ...overrides,
  }
}

describe('courseHint', () => {
  it('describe una asignatura aprobada', () => {
    expect(courseHint(summary({ status: 'PASSED', finalGrade: 4.8 }))).toBe(
      'Aprobada con nota final 4,8.',
    )
  })

  it('describe una asignatura reprobada', () => {
    expect(courseHint(summary({ status: 'FAILED', finalGrade: 3.9 }))).toBe(
      'Reprobada con nota final 3,9.',
    )
  })

  it('describe una aprobación asegurada', () => {
    expect(courseHint(summary({ status: 'SECURED' }))).toContain('asegurada')
  })

  it('avisa cuando ya no alcanza para aprobar', () => {
    expect(courseHint(summary({ status: 'IMPOSSIBLE' }))).toContain('7,0')
  })

  it('indica la nota necesaria cuando todavía no hay notas', () => {
    expect(courseHint(summary({ requiredAverage: 4 }))).toBe(
      'Necesitas un promedio de 4,0 para aprobar.',
    )
  })

  it('indica el promedio actual y lo que falta cuando hay notas', () => {
    expect(courseHint(summary({ currentAverage: 5.5, requiredAverage: 3.2 }))).toBe(
      'Promedio actual 5,5. Necesitas 3,2 en lo que falta.',
    )
  })
})

describe('estados', () => {
  it('tienen texto y color todos los estados', () => {
    const statuses: CourseStatus[] = ['PASSED', 'FAILED', 'SECURED', 'IN_PROGRESS', 'IMPOSSIBLE']

    for (const status of statuses) {
      expect(statusLabel[status]).toBeTruthy()
      expect(statusClass[status]).toBeTruthy()
    }
  })
})
