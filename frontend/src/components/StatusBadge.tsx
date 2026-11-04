import type { CourseStatus } from '../api/summary'
import { statusClass, statusLabel } from '../utils/grades'

export function StatusBadge({ status }: { status: CourseStatus }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${statusClass[status]}`}>
      {statusLabel[status]}
    </span>
  )
}
