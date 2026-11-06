export type MeetingStatus = 'SCHEDULED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
export type MeetingType = 'INSTANT' | 'SCHEDULED'
export type ParticipantStatus = 'INVITED' | 'ACCEPTED' | 'JOINED' | 'LEFT' | 'DECLINED' | 'REMOVED'

export interface Meeting {
  id: string
  title: string
  description: string | null
  roomCode: string
  meetingType: MeetingType
  status: MeetingStatus
  maxParticipants: number
  organizationId: string | null
  projectId: string | null
  createdBy: string
  scheduledStart: string | null
  scheduledEnd: string | null
  actualStart: string | null
  actualEnd: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface CreateMeetingInput {
  title: string
  description?: string
  meetingType: MeetingType
  maxParticipants: number
  organizationId?: string
  projectId?: string
  scheduledStart?: string
  scheduledEnd?: string
}

export interface JoinMeetingResponse {
  meetingId: string
  roomCode: string
  title: string
  jitsiServerUrl: string
  displayName: string
  email: string
  role: 'ORGANIZER' | 'CO_HOST' | 'PARTICIPANT'
}

export interface MeetingSignal {
  id: string
  meetingId: string
  type: string
  senderPeerId: string
  senderName: string
  targetPeerId?: string | null
  payload?: any
  timestamp: number
}

export interface SignalInput {
  type: string
  senderPeerId: string
  senderName?: string
  targetPeerId?: string | null
  payload?: any
}

export interface MeetingParticipant {
  id: string
  meetingId: string
  userId: string | null
  displayName: string
  email: string | null
  role: 'ORGANIZER' | 'CO_HOST' | 'PARTICIPANT'
  status: ParticipantStatus
  invitedBy: string | null
  joinedAt: string | null
  leftAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: string
  meetingId: string
  senderId: string
  senderName: string
  content: string
  messageType: string
  createdAt: string
}

// ==========================================
// Organizations & Identity Types
// ==========================================

export interface User {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  displayName: string | null
  profilePictureUrl: string | null
  timeZone: string | null
  language: string | null
  phoneNumber: string | null
  emailVerified: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export type OrganizationRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'GUEST'
export type MembershipStatus = 'ACTIVE' | 'SUSPENDED' | 'INVITED' | 'DECLINED'
export type EntityType = 'CORPORATE' | 'STARTUP'

export interface Organization {
  id: string
  name: string
  slug?: string
  status?: string
  description: string | null
  website: string | null
  industry: string | null
  size: string | null
  country: string | null
  city: string | null
  address: string | null
  logoUrl: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface OrganizationProfile {
  organizationId: string
  entityType: EntityType
  legalName: string
  registrationNumber?: string | null
  taxId?: string | null
  jurisdiction?: string | null
  incorporationDate?: string | null
  industry?: string | null
  companySize?: string | null
  stage?: string | null
  website?: string | null
  linkedinUrl?: string | null
  country?: string | null
  city?: string | null
  address?: string | null
  postalCode?: string | null
  signatoryName?: string | null
  signatoryTitle?: string | null
  signatoryEmail?: string | null
  signatoryPhone?: string | null
  incorporationDocName?: string | null
  taxDocName?: string | null
  pitchDeckDocName?: string | null
  allowedEmailDomain?: string | null
  enforceCompanyDomain?: boolean | null
  verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED'
  createdAt: string
  updatedAt: string
}

export interface OnboardPayload {
  name: string
  slug?: string
  entityType: EntityType
  legalName?: string
  registrationNumber?: string
  taxId?: string
  jurisdiction?: string
  incorporationDate?: string
  industry?: string
  companySize?: string
  stage?: string
  website?: string
  linkedinUrl?: string
  country?: string
  city?: string
  address?: string
  postalCode?: string
  signatoryName?: string
  signatoryTitle?: string
  signatoryEmail?: string
  signatoryPhone?: string
  incorporationDocName?: string
  taxDocName?: string
  pitchDeckDocName?: string
  planId?: string
  billingInterval?: 'MONTHLY' | 'ANNUAL'
}

export interface OnboardResponse {
  organization: Organization
  profile: OrganizationProfile
}

export interface OrganizationMembership {
  id: string
  organizationId: string
  userId: string
  role: OrganizationRole
  status: MembershipStatus
  invitedBy: string | null
  joinedAt: string | null
  createdAt: string
  updatedAt: string
  user?: User
}

export interface Invitation {
  id: string
  organizationId: string
  inviteeEmail: string
  inviterId?: string
  intendedRole: OrganizationRole
  status: string
  expiresAt?: string
  decidedAt?: string | null
}

export interface SentEmail {
  id: string
  organizationId: string
  recipientEmail: string
  subject: string
  invitationToken: string
  status: string
  sentAt: string
}

// ==========================================
// Client Types
// ==========================================

export type ClientStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'

export interface Client {
  id: string
  organizationId: string
  name: string
  description: string | null
  industry: string | null
  website: string | null
  email: string | null
  phone: string | null
  address: string | null
  city: string | null
  country: string | null
  status: ClientStatus
  createdBy: string
  createdAt: string
  updatedAt: string
}

// ==========================================
// Project Types
// ==========================================

export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED'
export type ProjectRole = 'PROJECT_OWNER' | 'PROJECT_ADMIN' | 'COLLABORATOR' | 'VIEWER'
export type ParticipationStatus = 'INVITED' | 'ACTIVE' | 'SUSPENDED' | 'LEFT'

export interface Project {
  id: string
  name: string
  description: string | null
  clientId: string | null
  ownerOrganizationId: string
  status: ProjectStatus
  startDate: string | null
  targetEndDate: string | null
  actualEndDate: string | null
  budget: number | null
  currency: string | null
  createdBy: string
  createdAt: string
  updatedAt: string
  client?: Client
}

export interface ProjectParticipation {
  id: string
  projectId: string
  organizationId: string
  role: ProjectRole
  status: ParticipationStatus
  invitedBy: string | null
  joinedAt: string | null
  createdAt: string
  updatedAt: string
  organization?: Organization
}

// ==========================================
// Work Management Types
// ==========================================

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface Task {
  id: string
  projectId: string
  workstreamId: string | null
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  assigneeId: string | null
  reporterId: string
  dueDate: string | null
  estimatedHours: number | null
  actualHours: number | null
  createdAt: string
  updatedAt: string
  assignee?: User
  reporter?: User
}

export interface Workstream {
  id: string
  projectId: string
  name: string
  description: string | null
  startDate: string | null
  endDate: string | null
  status: string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface Milestone {
  id: string
  projectId: string
  name: string
  description: string | null
  dueDate: string
  completedAt: string | null
  status: string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface TaskComment {
  id: string
  taskId: string
  userId: string
  content: string
  createdAt: string
  updatedAt: string
  user?: User
}

// ==========================================
// Document Types
// ==========================================

export type DocumentStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'ARCHIVED'
export type ReviewStatus = 'PENDING' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED'

export interface Document {
  id: string
  projectId: string
  name: string
  description: string | null
  fileKey: string
  mimeType: string
  sizeBytes: number
  status: DocumentStatus
  currentVersionId: string | null
  uploadedBy: string
  createdAt: string
  updatedAt: string
}

export interface DocumentVersion {
  id: string
  documentId: string
  versionNumber: number
  fileKey: string
  sizeBytes: number
  changes: string | null
  status: DocumentStatus
  uploadedBy: string
  createdAt: string
}

export interface DocumentReview {
  id: string
  versionId: string
  reviewerId: string
  status: ReviewStatus
  feedback: string | null
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
  reviewer?: User
}

// ==========================================
// Phase 7: Finance & Subscriptions Types
// ==========================================

export type PlanTier = 'FREE' | 'STARTER' | 'PROFESSIONAL' | 'ENTERPRISE' | 'CUSTOM'
export type BillingInterval = 'MONTHLY' | 'ANNUAL'
export type SubscriptionStatus = 'TRIAL' | 'ACTIVE' | 'PAST_DUE' | 'SUSPENDED' | 'CANCELLED'

export interface SubscriptionPlan {
  id: string
  name: string
  displayName: string
  description: string
  tier: PlanTier
  billingInterval: BillingInterval
  price: number
  currency: string
  maxMembers: number
  maxProjects: number
  storageLimitMb: number
  maxMeetingParticipants: number
  maxMeetingDurationMin: number
  aiRequestsLimit: number
  documentLimit: number
  isActive: boolean
  trialDays: number
  features: string[]
}

export interface OrganizationSubscription {
  id: string
  organizationId: string
  planId: string
  planName: string
  planDisplayName: string
  status: SubscriptionStatus
  billingInterval: BillingInterval
  currentPeriodStart: string
  currentPeriodEnd: string
  trialEnd: string | null
  cancelledAt: string | null
  createdAt: string
  updatedAt: string
}

export type UsageMetric =
  | 'MEMBERS'
  | 'STORAGE_MB'
  | 'PROJECTS'
  | 'MEETINGS'
  | 'MEETING_MINUTES'
  | 'AI_REQUESTS'
  | 'DOCUMENTS'
  | 'API_CALLS'

export interface SubscriptionUsage {
  metric: UsageMetric
  currentValue: number
  limit: number
  remainingQuota: number
  periodStart: string
  periodEnd: string
}

export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE' | 'VOID' | 'PARTIALLY_PAID'

export interface InvoiceLineItem {
  id: string
  invoiceId: string
  description: string
  quantity: number
  unitPrice: number
  amount: number
  sortOrder: number
}

export interface Invoice {
  id: string
  invoiceNumber: string
  organizationId: string
  subscriptionId: string | null
  status: InvoiceStatus
  currency: string
  subtotal: number
  taxRate: number
  taxAmount: number
  discountAmount: number
  total: number
  amountPaid: number
  amountDue: number
  billingPeriodStart: string | null
  billingPeriodEnd: string | null
  issuedAt: string | null
  dueDate: string | null
  paidAt: string | null
  notes: string | null
  lineItems: InvoiceLineItem[]
  createdAt: string
  updatedAt: string
}

export type PaymentMethod = 'MANUAL' | 'BANK_TRANSFER' | 'CARD' | 'OTHER'
export type PaymentStatus = 'PENDING' | 'CONFIRMED' | 'FAILED' | 'REFUNDED'

export interface Payment {
  id: string
  organizationId: string
  invoiceId: string | null
  paymentMethod: PaymentMethod
  status: PaymentStatus
  amount: number
  currency: string
  referenceNumber: string | null
  idempotencyKey: string | null
  confirmedBy: string | null
  confirmedAt: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// ==========================================
// Phase 8: Notifications, Activity & Audit Types
// ==========================================

export type NotificationCategory =
  | 'ORGANIZATION'
  | 'CLIENT'
  | 'PROJECT'
  | 'WORK'
  | 'DOCUMENT'
  | 'COMMUNICATION'
  | 'FINANCE'
  | 'SYSTEM'

export type NotificationPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface NotificationItem {
  id: string
  recipientId: string
  organizationId: string | null
  eventId: string | null
  category: string
  priority: string
  title: string
  message: string
  eventType: string
  referenceId: string | null
  referenceType: string | null
  extraData: string | null
  readFlag: boolean
  createdAt: string
  expiresAt: string | null
}

export interface NotificationPreference {
  id: string
  userId: string
  inAppEnabled: boolean
  emailEnabled: boolean
  realTimeEnabled: boolean
  categoryPreferences: string
  quietHoursStart: string | null
  quietHoursEnd: string | null
  timeZone: string
  digestFrequency: string
  updatedAt: string
}

export interface ActivityFeedItem {
  id: string
  organizationId: string
  projectId: string | null
  actorId: string
  category: string
  eventType: string
  resourceType: string
  resourceId: string | null
  description: string
  metadata: string | null
  correlationId: string | null
  createdAt: string
}

export interface AuditLogItem {
  id: string
  organizationId: string
  actorId: string
  action: string
  resourceType: string
  resourceId: string | null
  sourceService: string
  outcome: string
  correlationId: string | null
  metadata: string | null
  createdAt: string
}


