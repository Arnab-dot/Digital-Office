// Organization roles
export type OrganizationRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'GUEST'

// Project roles
export type ProjectRole = 'PROJECT_OWNER' | 'PROJECT_ADMIN' | 'COLLABORATOR' | 'VIEWER'

export interface OrganizationMembership {
  organizationId: string
  userId: string
  role: OrganizationRole
  status: 'ACTIVE' | 'SUSPENDED' | 'INVITED' | 'DECLINED'
}

export interface ProjectParticipation {
  projectId: string
  userId: string
  role: ProjectRole
  organizationId: string
}

// Capability definitions
export type Capability =
  // Organization capabilities
  | 'org:manage'
  | 'org:invite'
  | 'org:view-members'
  | 'org:manage-members'
  | 'org:view-finance'
  | 'org:manage-finance'
  | 'org:view-audit'
  // Client capabilities
  | 'client:create'
  | 'client:edit'
  | 'client:archive'
  | 'client:view'
  // Project capabilities
  | 'project:create'
  | 'project:edit'
  | 'project:archive'
  | 'project:view'
  | 'project:invite'
  | 'project:manage-participants'
  // Work capabilities
  | 'work:create'
  | 'work:edit'
  | 'work:assign'
  | 'work:view'
  // Document capabilities
  | 'document:upload'
  | 'document:edit'
  | 'document:review'
  | 'document:approve'
  | 'document:view'
  // Meeting capabilities
  | 'meeting:create'
  | 'meeting:manage'
  | 'meeting:join'
