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

export type Reminder = {
  id: number
  assessmentId: number
  daysBefore: number
  sent: boolean
  sentAt: string | null
}

export function fetchUpcomingReminders(): Promise<ReminderNotification[]> {
  return apiFetch<ReminderNotification[]>('/api/reminders/upcoming')
}

export function dismissReminder(id: number): Promise<void> {
  return apiFetch<void>(`/api/reminders/${id}/dismiss`, { method: 'POST' })
}

export function listReminders(assessmentId: number): Promise<Reminder[]> {
  return apiFetch<Reminder[]>(`/api/assessments/${assessmentId}/reminders`)
}

export function createReminder(assessmentId: number, daysBefore: number): Promise<Reminder> {
  return apiFetch<Reminder>(`/api/assessments/${assessmentId}/reminders`, {
    method: 'POST',
    body: { daysBefore },
  })
}

export function deleteReminder(id: number): Promise<void> {
  return apiFetch<void>(`/api/reminders/${id}`, { method: 'DELETE' })
}
