import type {
  ActivityFeedItem,
  AuditLogItem,
  ChatMessage,
  Client,
  CreateMeetingInput,
  Document,
  DocumentReview,
  DocumentVersion,
  Invitation,
  Invoice,
  JoinMeetingResponse,
  Meeting,
  MeetingParticipant,
  MeetingSignal,
  Milestone,
  NotificationItem,
  NotificationPreference,
  OnboardPayload,
  OnboardResponse,
  Organization,
  OrganizationMembership,
  OrganizationProfile,
  OrganizationRole,
  OrganizationSubscription,
  PageResponse,
  Payment,
  PaymentMethod,
  Project,
  ProjectParticipation,
  ProjectRole,
  ProjectStatus,
  ReviewStatus,
  SentEmail,
  SignalInput,
  SubscriptionPlan,
  SubscriptionUsage,
  Task,
  TaskComment,
  TaskPriority,
  TaskStatus,
  Workstream,
} from './types'
import { refreshAccessToken } from './lib/auth/keycloak'

const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
const accessTokenKey = 'digital-office.access-token'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

function getAccessToken(): string | null {
  return sessionStorage.getItem(accessTokenKey) ?? localStorage.getItem(accessTokenKey)
}

let refreshPromise: Promise<string | null> | null = null

async function request<T>(path: string, init: RequestInit = {}, isRetry = false): Promise<T> {
  const headers = new Headers(init.headers)
  const token = getAccessToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  let response: Response
  try {
    response = await fetch(`${baseUrl}${path}`, { ...init, headers })
  } catch {
    throw new ApiError(0, 'Unable to reach the server. Check that the API gateway is running.')
  }

  if (response.status === 401 && !isRetry) {
    if (!refreshPromise) {
      refreshPromise = refreshAccessToken().finally(() => {
        refreshPromise = null
      })
    }
    const newToken = await refreshPromise
    if (newToken) {
      return request<T>(path, init, true)
    } else {
      if (!window.location.pathname.startsWith('/sign-in') && !window.location.pathname.startsWith('/register')) {
        window.location.assign('/sign-in')
      }
    }
  }

  if (!response.ok) {
    const fallback = `Request to ${path} failed with status ${response.status}.`
    try {
      const problem = (await response.json()) as { detail?: string; title?: string; message?: string; error?: string }
      throw new ApiError(response.status, problem.detail ?? problem.message ?? problem.title ?? problem.error ?? fallback)
    } catch (error) {
      if (error instanceof ApiError) throw error
      throw new ApiError(response.status, fallback)
    }
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const meetingsApi = {
  list: () => request<Meeting[]>('/api/v1/meetings'),
  get: (meetingId: string) => request<Meeting>(`/api/v1/meetings/${meetingId}`),
  create: (input: CreateMeetingInput) =>
    request<Meeting>('/api/v1/meetings', { method: 'POST', body: JSON.stringify(input) }),
  join: (meetingId: string) => request<JoinMeetingResponse>(`/api/v1/meetings/${meetingId}/join`, { method: 'POST' }),
  leave: (meetingId: string) => request<void>(`/api/v1/meetings/${meetingId}/leave`, { method: 'POST' }),
  end: (meetingId: string) => request<void>(`/api/v1/meetings/${meetingId}/end`, { method: 'POST' }),
  participants: (meetingId: string) => request<MeetingParticipant[]>(`/api/v1/meetings/${meetingId}/participants`),
  chat: (meetingId: string) => request<ChatMessage[]>(`/api/v1/meetings/${meetingId}/chat`),
  sendMessage: (meetingId: string, content: string) =>
    request<ChatMessage>(`/api/v1/meetings/${meetingId}/chat`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
  sendSignal: (meetingId: string, input: SignalInput) =>
    request<MeetingSignal>(`/api/v1/meetings/${meetingId}/signal`, {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  getSignals: (meetingId: string, peerId?: string, since = 0) =>
    request<MeetingSignal[]>(
      `/api/v1/meetings/${meetingId}/signal?peerId=${encodeURIComponent(peerId || '')}&since=${since}`
    ),
}

export const meetingApi = meetingsApi

export const financeApi = {
  listPlans: () => request<SubscriptionPlan[]>('/api/v1/finance/plans'),
  getPlan: (id: string) => request<SubscriptionPlan>(`/api/v1/finance/plans/${id}`),

  getSubscription: (orgId: string) =>
    request<OrganizationSubscription>(`/api/v1/organizations/${orgId}/subscription`),
  createSubscription: (orgId: string, planId: string, billingInterval: 'MONTHLY' | 'ANNUAL' = 'MONTHLY') =>
    request<OrganizationSubscription>(`/api/v1/organizations/${orgId}/subscription`, {
      method: 'POST',
      body: JSON.stringify({ planId, billingInterval }),
    }),
  changePlan: (orgId: string, newPlanId: string) =>
    request<OrganizationSubscription>(`/api/v1/organizations/${orgId}/subscription/change-plan`, {
      method: 'POST',
      body: JSON.stringify({ newPlanId }),
    }),
  cancelSubscription: (orgId: string) =>
    request<OrganizationSubscription>(`/api/v1/organizations/${orgId}/subscription/cancel`, {
      method: 'POST',
    }),
  renewSubscription: (orgId: string) =>
    request<OrganizationSubscription>(`/api/v1/organizations/${orgId}/subscription/renew`, {
      method: 'POST',
    }),
  reactivateSubscription: (orgId: string) =>
    request<OrganizationSubscription>(`/api/v1/organizations/${orgId}/subscription/reactivate`, {
      method: 'POST',
    }),

  getUsage: (orgId: string) =>
    request<SubscriptionUsage[]>(`/api/v1/organizations/${orgId}/usage`),

  listInvoices: (orgId: string, page = 0, size = 20) =>
    request<PageResponse<Invoice>>(`/api/v1/organizations/${orgId}/invoices?page=${page}&size=${size}`),
  getInvoice: (orgId: string, invoiceId: string) =>
    request<Invoice>(`/api/v1/organizations/${orgId}/invoices/${invoiceId}`),
  downloadInvoicePdfUrl: (orgId: string, invoiceId: string) =>
    `${baseUrl}/api/v1/organizations/${orgId}/invoices/${invoiceId}/pdf`,

  listPayments: (orgId: string, page = 0, size = 20) =>
    request<PageResponse<Payment>>(`/api/v1/organizations/${orgId}/payments?page=${page}&size=${size}`),
  recordPayment: (
    orgId: string,
    input: {
      invoiceId?: string
      paymentMethod: PaymentMethod
      amount: number
      currency?: string
      referenceNumber?: string
      idempotencyKey?: string
      notes?: string
    }
  ) =>
    request<Payment>(`/api/v1/organizations/${orgId}/payments`, {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  confirmPayment: (orgId: string, paymentId: string) =>
    request<Payment>(`/api/v1/organizations/${orgId}/payments/${paymentId}/confirm`, {
      method: 'POST',
    }),
}

export const notificationApi = {
  list: (readStatus: 'all' | 'unread' | 'read' = 'all', page = 0, size = 20) =>
    request<PageResponse<NotificationItem>>(
      `/api/v1/notifications?readStatus=${readStatus}&page=${page}&size=${size}`
    ),

  unreadCount: () => request<{ unreadCount: number }>('/api/v1/notifications/unread-count'),

  markRead: (id: string) =>
    request<NotificationItem>(`/api/v1/notifications/${id}/read`, {
      method: 'PATCH',
    }),

  markAllRead: () =>
    request<void>('/api/v1/notifications/read-all', {
      method: 'POST',
    }),

  getPreferences: () =>
    request<NotificationPreference>('/api/v1/notifications/preferences'),

  updatePreferences: (prefs: {
    inAppEnabled: boolean
    emailEnabled: boolean
    realTimeEnabled: boolean
    categoryPreferences: string
    quietHoursStart?: string | null
    quietHoursEnd?: string | null
    timeZone?: string
    digestFrequency?: string
  }) =>
    request<NotificationPreference>('/api/v1/notifications/preferences', {
      method: 'PUT',
      body: JSON.stringify(prefs),
    }),
}

export const activityApi = {
  getOrgActivity: (orgId: string, category?: string, page = 0, size = 20) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (category && category !== 'ALL') params.append('category', category)
    return request<PageResponse<ActivityFeedItem>>(
      `/api/v1/organizations/${orgId}/activity?${params.toString()}`
    )
  },

  getProjectActivity: (projectId: string, category?: string, page = 0, size = 20) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (category && category !== 'ALL') params.append('category', category)
    return request<PageResponse<ActivityFeedItem>>(
      `/api/v1/projects/${projectId}/activity?${params.toString()}`
    )
  },
}

export const auditApi = {
  getOrgAuditLogs: (
    orgId: string,
    action?: string,
    actorId?: string,
    page = 0,
    size = 20
  ) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (action && action.trim()) params.append('action', action.trim())
    if (actorId && actorId.trim()) params.append('actorId', actorId.trim())
    return request<PageResponse<AuditLogItem>>(
      `/api/v1/organizations/${orgId}/audit-logs?${params.toString()}`
    )
  },
}

// ==========================================
// Organizations API
// ==========================================

export const organizationApi = {
  list: () => request<Organization[]>('/api/v1/organizations'),
  
  get: (id: string) => request<Organization>(`/api/v1/organizations/${id}`),
  
  create: (data: {
    name: string
    slug?: string
    description?: string
    website?: string
    industry?: string
    size?: string
    country?: string
    city?: string
    address?: string
  }) => {
    const cleanSlug = (data.slug || data.name.toLowerCase().trim().replace(/[^a-z0-9-]+/g, '-').replace(/^-+|-+$/g, '')).slice(0, 80)
    const finalSlug = cleanSlug.length >= 3 ? cleanSlug : `${cleanSlug || 'org'}-${Math.random().toString(36).substring(2, 6)}`
    return request<Organization>('/api/v1/organizations', {
      method: 'POST',
      body: JSON.stringify({
        ...data,
        slug: finalSlug,
      }),
    })
  },
  
  update: (id: string, data: Partial<Organization>) =>
    request<Organization>(`/api/v1/organizations/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  
  delete: (id: string) =>
    request<void>(`/api/v1/organizations/${id}`, { method: 'DELETE' }),
  
  // Members
  listMembers: (orgId: string) =>
    request<OrganizationMembership[]>(`/api/v1/organizations/${orgId}/members`),
  
  getMember: (orgId: string, memberId: string) =>
    request<OrganizationMembership>(`/api/v1/organizations/${orgId}/members/${memberId}`),
  
  updateMemberRole: (orgId: string, memberId: string, role: OrganizationRole) =>
    request<OrganizationMembership>(`/api/v1/organizations/${orgId}/members/${memberId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    }),
  
  suspendMember: (orgId: string, memberId: string) =>
    request<OrganizationMembership>(`/api/v1/organizations/${orgId}/members/${memberId}/suspend`, {
      method: 'POST',
    }),
  
  restoreMember: (orgId: string, memberId: string) =>
    request<OrganizationMembership>(`/api/v1/organizations/${orgId}/members/${memberId}/restore`, {
      method: 'POST',
    }),
  
  removeMember: (orgId: string, memberId: string) =>
    request<void>(`/api/v1/organizations/${orgId}/members/${memberId}`, {
      method: 'DELETE',
    }),
  
  // Invitations
  createInvitation: (orgId: string, email: string, role: OrganizationRole) =>
    request<Invitation>(`/api/v1/organizations/${orgId}/invitations`, {
      method: 'POST',
      body: JSON.stringify({ email, role }),
    }),
  
  listInvitations: (orgId: string) =>
    request<Invitation[]>(`/api/v1/organizations/${orgId}/invitations`),

  listSentEmails: (orgId: string) =>
    request<SentEmail[]>(`/api/v1/organizations/${orgId}/emails`),
  
  acceptInvitation: (token: string) =>
    request<void>(`/api/v1/invitations/${token}/accept`, { method: 'POST' }),
  
  declineInvitation: (token: string) =>
    request<void>(`/api/v1/invitations/${token}/decline`, { method: 'POST' }),
  
  revokeInvitation: (orgId: string, invitationId: string) =>
    request<void>(`/api/v1/organizations/${orgId}/invitations/${invitationId}/revoke`, {
      method: 'POST',
    }),

  // Corporate & Startup Onboarding & Profile
  onboard: (data: OnboardPayload) => {
    const cleanSlug = (data.slug || data.name.toLowerCase().trim().replace(/[^a-z0-9-]+/g, '-').replace(/^-+|-+$/g, '')).slice(0, 80)
    const finalSlug = cleanSlug.length >= 3 ? cleanSlug : `${cleanSlug || 'org'}-${Math.random().toString(36).substring(2, 6)}`
    return request<OnboardResponse>('/api/v1/organizations/onboard', {
      method: 'POST',
      body: JSON.stringify({
        ...data,
        slug: finalSlug,
      }),
    })
  },

  getProfile: (orgId: string) =>
    request<OrganizationProfile | null>(`/api/v1/organizations/${orgId}/profile`),

  updateProfile: (orgId: string, data: Partial<OrganizationProfile>) =>
    request<OrganizationProfile>(`/api/v1/organizations/${orgId}/profile`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
}

// ==========================================
// Clients API
// ==========================================

export const clientApi = {
  list: (orgId: string, page = 0, size = 20) =>
    request<PageResponse<Client>>(`/api/v1/organizations/${orgId}/clients?page=${page}&size=${size}`),
  
  get: (id: string) => request<Client>(`/api/v1/clients/${id}`),
  
  create: (orgId: string, data: {
    name: string
    description?: string
    industry?: string
    website?: string
    email?: string
    phone?: string
    address?: string
    city?: string
    country?: string
  }) =>
    request<Client>(`/api/v1/organizations/${orgId}/clients`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  
  update: (id: string, data: Partial<Client>) =>
    request<Client>(`/api/v1/clients/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  
  archive: (id: string) =>
    request<Client>(`/api/v1/clients/${id}/archive`, { method: 'POST' }),
  
  delete: (id: string) =>
    request<void>(`/api/v1/clients/${id}`, { method: 'DELETE' }),
  
  search: (orgId: string, query: string, page = 0, size = 20) =>
    request<PageResponse<Client>>(
      `/api/v1/organizations/${orgId}/clients/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
    ),
}

// ==========================================
// Projects API
// ==========================================

export const projectApi = {
  list: (page = 0, size = 20) =>
    request<Project[]>(`/api/v1/projects?page=${page}&size=${size}`),
  
  get: (id: string) => request<Project>(`/api/v1/projects/${id}`),
  
  create: (data: {
    name: string
    description?: string
    clientId?: string
    status?: ProjectStatus
    startDate?: string
    targetEndDate?: string
    budget?: number
    currency?: string
  }) =>
    request<Project>('/api/v1/projects', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  
  update: (id: string, data: Partial<Project>) =>
    request<Project>(`/api/v1/projects/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  
  delete: (id: string) =>
    request<void>(`/api/v1/projects/${id}`, { method: 'DELETE' }),
  
  // Participants
  listParticipants: (projectId: string) =>
    request<ProjectParticipation[]>(`/api/v1/projects/${projectId}/participants`),
  
  inviteOrganization: (projectId: string, organizationId: string, role: ProjectRole) =>
    request<ProjectParticipation>(`/api/v1/projects/${projectId}/participants`, {
      method: 'POST',
      body: JSON.stringify({ organizationId, role }),
    }),
  
  updateParticipantRole: (projectId: string, participationId: string, role: ProjectRole) =>
    request<ProjectParticipation>(
      `/api/v1/projects/${projectId}/participants/${participationId}/role`,
      {
        method: 'PATCH',
        body: JSON.stringify({ role }),
      }
    ),
  
  removeParticipant: (projectId: string, participationId: string) =>
    request<void>(`/api/v1/projects/${projectId}/participants/${participationId}`, {
      method: 'DELETE',
    }),
  
  search: (query: string, page = 0, size = 20) =>
    request<PageResponse<Project>>(
      `/api/v1/search/projects?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
    ),
}

// ==========================================
// Work/Tasks API
// ==========================================

export const workApi = {
  // Tasks
  listTasks: (projectId?: string, page = 0, size = 20) => {
    const url = projectId
      ? `/api/v1/projects/${projectId}/tasks?page=${page}&size=${size}`
      : `/api/v1/tasks?page=${page}&size=${size}`
    return request<PageResponse<Task>>(url)
  },
  
  getTask: (id: string) => request<Task>(`/api/v1/tasks/${id}`),
  
  createTask: (projectId: string, data: {
    title: string
    description?: string
    status?: TaskStatus
    priority?: TaskPriority
    assigneeId?: string
    workstreamId?: string
    dueDate?: string
    estimatedHours?: number
  }) =>
    request<Task>(`/api/v1/projects/${projectId}/tasks`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  
  updateTask: (id: string, data: Partial<Task>) =>
    request<Task>(`/api/v1/tasks/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  
  deleteTask: (id: string) =>
    request<void>(`/api/v1/tasks/${id}`, { method: 'DELETE' }),
  
  assignTask: (id: string, assigneeId: string) =>
    request<Task>(`/api/v1/tasks/${id}/assign`, {
      method: 'POST',
      body: JSON.stringify({ assigneeId }),
    }),
  
  // Comments
  listComments: (taskId: string) =>
    request<TaskComment[]>(`/api/v1/tasks/${taskId}/comments`),
  
  addComment: (taskId: string, content: string) =>
    request<TaskComment>(`/api/v1/tasks/${taskId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
  
  // Workstreams
  listWorkstreams: (projectId: string) =>
    request<Workstream[]>(`/api/v1/projects/${projectId}/workstreams`),
  
  createWorkstream: (projectId: string, data: {
    name: string
    description?: string
    startDate?: string
    endDate?: string
  }) =>
    request<Workstream>(`/api/v1/projects/${projectId}/workstreams`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  
  // Milestones
  listMilestones: (projectId: string) =>
    request<Milestone[]>(`/api/v1/projects/${projectId}/milestones`),
  
  createMilestone: (projectId: string, data: {
    name: string
    description?: string
    dueDate: string
  }) =>
    request<Milestone>(`/api/v1/projects/${projectId}/milestones`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  
  search: (query: string, page = 0, size = 20) =>
    request<PageResponse<Task>>(
      `/api/v1/search/tasks?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
    ),
}

// ==========================================
// Documents API
// ==========================================

export const documentApi = {
  list: (projectId: string, page = 0, size = 20) =>
    request<PageResponse<Document>>(
      `/api/v1/projects/${projectId}/documents?page=${page}&size=${size}`
    ),
  
  get: (id: string) => request<Document>(`/api/v1/documents/${id}`),
  
  upload: async (projectId: string, file: File, description?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (description) formData.append('description', description)
    
    const token = getAccessToken()
    const headers = new Headers()
    if (token) headers.set('Authorization', `Bearer ${token}`)
    
    const response = await fetch(`${baseUrl}/api/v1/projects/${projectId}/documents`, {
      method: 'POST',
      headers,
      body: formData,
    })
    
    if (!response.ok) throw new ApiError(response.status, 'Upload failed')
    return (await response.json()) as Document
  },
  
  update: (id: string, data: { name?: string; description?: string }) =>
    request<Document>(`/api/v1/documents/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  
  delete: (id: string) =>
    request<void>(`/api/v1/documents/${id}`, { method: 'DELETE' }),
  
  // Versions
  listVersions: (documentId: string) =>
    request<DocumentVersion[]>(`/api/v1/documents/${documentId}/versions`),
  
  uploadVersion: async (documentId: string, file: File, changes?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (changes) formData.append('changes', changes)
    
    const token = getAccessToken()
    const headers = new Headers()
    if (token) headers.set('Authorization', `Bearer ${token}`)
    
    const response = await fetch(`${baseUrl}/api/v1/documents/${documentId}/versions`, {
      method: 'POST',
      headers,
      body: formData,
    })
    
    if (!response.ok) throw new ApiError(response.status, 'Version upload failed')
    return (await response.json()) as DocumentVersion
  },
  
  // Reviews
  submitForReview: (documentId: string, versionId: string, reviewerIds: string[]) =>
    request<void>(`/api/v1/documents/${documentId}/versions/${versionId}/submit-for-review`, {
      method: 'POST',
      body: JSON.stringify({ reviewerIds }),
    }),
  
  listReviews: (documentId: string, versionId: string) =>
    request<DocumentReview[]>(`/api/v1/documents/${documentId}/versions/${versionId}/reviews`),
  
  submitReview: (documentId: string, versionId: string, reviewId: string, status: ReviewStatus, feedback?: string) =>
    request<DocumentReview>(`/api/v1/documents/${documentId}/versions/${versionId}/reviews/${reviewId}`, {
      method: 'PUT',
      body: JSON.stringify({ status, feedback }),
    }),
  
  downloadUrl: (documentId: string, versionId?: string) =>
    versionId
      ? `${baseUrl}/api/v1/documents/${documentId}/versions/${versionId}/download`
      : `${baseUrl}/api/v1/documents/${documentId}/download`,
  
  search: (query: string, page = 0, size = 20) =>
    request<PageResponse<Document>>(
      `/api/v1/search/documents?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
    ),
}

// ==========================================
// Dashboard API
// ==========================================

export interface DashboardSummary {
  activeTaskCount: number
  upcomingMilestones: Array<{ id: string; title: string; dueDate: string }>
  pendingInvitations: Array<{ id: string; projectId: string }>
  pendingReviews: Array<{ id: string; documentId: string }>
  unreadNotificationCount: number
  warnings: string[]
}

export const dashboardApi = {
  getSummary: () => request<DashboardSummary>('/api/v1/dashboard'),
}
