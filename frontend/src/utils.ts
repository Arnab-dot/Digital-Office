import type { Meeting, MeetingStatus } from './types'

export function formatMeetingTime(meeting: Meeting): string {
  const date = meeting.status === 'ACTIVE' ? meeting.actualStart ?? meeting.scheduledStart : meeting.scheduledStart
  if (!date) return meeting.meetingType === 'INSTANT' ? 'Starts immediately' : 'Time to be confirmed'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(date))
}

export function formatShortTime(value: string): string {
  return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(new Date(value))
}

export function statusLabel(status: MeetingStatus): string {
  return status.charAt(0) + status.slice(1).toLowerCase()
}

export function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('') || '?'
}

export function toLocalDateTimeInput(date: Date): string {
  const timezoneOffset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 16)
}
