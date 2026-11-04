import type { AssessmentStatus } from '../api/assessments'
import { assessmentStatusClass, assessmentStatusLabel } from '../utils/labels'

export function AssessmentStatusBadge({ status }: { status: AssessmentStatus }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${assessmentStatusClass[status]}`}>
      {assessmentStatusLabel[status]}
    </span>
  )
}
