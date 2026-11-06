import type { OrganizationRole, ProjectRole, Capability } from './types'

// Organization role -> capabilities mapping
const orgRoleCapabilities: Record<OrganizationRole, Capability[]> = {
  OWNER: [
    'org:manage',
    'org:invite',
    'org:view-members',
    'org:manage-members',
    'org:view-finance',
    'org:manage-finance',
    'org:view-audit',
    'client:create',
    'client:edit',
    'client:archive',
    'client:view',
    'project:create',
    'project:edit',
    'project:archive',
    'project:view',
    'project:invite',
    'project:manage-participants',
    'work:create',
    'work:edit',
    'work:assign',
    'work:view',
    'document:upload',
    'document:edit',
    'document:review',
    'document:approve',
    'document:view',
    'meeting:create',
    'meeting:manage',
    'meeting:join',
  ],
  ADMIN: [
    'org:invite',
    'org:view-members',
    'org:manage-members',
    'org:view-finance',
    'client:create',
    'client:edit',
    'client:archive',
    'client:view',
    'project:create',
    'project:edit',
    'project:view',
    'project:invite',
    'work:create',
    'work:edit',
    'work:assign',
    'work:view',
    'document:upload',
    'document:edit',
    'document:review',
    'document:approve',
    'document:view',
    'meeting:create',
    'meeting:manage',
    'meeting:join',
  ],
  MEMBER: [
    'org:view-members',
    'client:view',
    'project:view',
    'work:create',
    'work:edit',
    'work:view',
    'document:upload',
    'document:edit',
    'document:review',
    'document:view',
    'meeting:create',
    'meeting:join',
  ],
  GUEST: [
    'project:view',
    'work:view',
    'document:view',
    'meeting:join',
  ],
}

// Project role -> capabilities mapping
const projectRoleCapabilities: Record<ProjectRole, Capability[]> = {
  PROJECT_OWNER: [
    'project:edit',
    'project:archive',
    'project:invite',
    'project:manage-participants',
    'work:create',
    'work:edit',
    'work:assign',
    'work:view',
    'document:upload',
    'document:edit',
    'document:review',
    'document:approve',
    'document:view',
    'meeting:create',
    'meeting:manage',
    'meeting:join',
  ],
  PROJECT_ADMIN: [
    'project:edit',
    'project:invite',
    'work:create',
    'work:edit',
    'work:assign',
    'work:view',
    'document:upload',
    'document:edit',
    'document:review',
    'document:approve',
    'document:view',
    'meeting:create',
    'meeting:manage',
    'meeting:join',
  ],
  COLLABORATOR: [
    'work:create',
    'work:edit',
    'work:view',
    'document:upload',
    'document:edit',
    'document:review',
    'document:view',
    'meeting:create',
    'meeting:join',
  ],
  VIEWER: ['work:view', 'document:view', 'meeting:join'],
}

export function getOrganizationCapabilities(role: OrganizationRole): Capability[] {
  return orgRoleCapabilities[role] || []
}

export function getProjectCapabilities(role: ProjectRole): Capability[] {
  return projectRoleCapabilities[role] || []
}

export function hasOrganizationCapability(role: OrganizationRole, capability: Capability): boolean {
  return getOrganizationCapabilities(role).includes(capability)
}

export function hasProjectCapability(role: ProjectRole, capability: Capability): boolean {
  return getProjectCapabilities(role).includes(capability)
}
