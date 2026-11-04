import { apiFetch } from './client'

export type ReminderNotification = {
  reminderId: number
  assessmentId: number
  assessmentName: string
  courseId: number
  courseName: string
  dueDate: string
  daysLeft: number
  daysBefore: number
}

export function fetchUpcomingReminders(): Promise<ReminderNotification[]> {
  return apiFetch<ReminderNotification[]>('/api/reminders/upcoming')
}

export function dismissReminder(id: number): Promise<void> {
  return apiFetch<void>(`/api/reminders/${id}/dismiss`, { method: 'POST' })
}
