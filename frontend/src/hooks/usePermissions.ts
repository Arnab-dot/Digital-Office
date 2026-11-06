import { useWorkspace } from '../app/providers/WorkspaceProvider'
import { hasOrganizationCapability, hasProjectCapability } from '../lib/permissions/capabilities'
import type { Capability } from '../lib/permissions/types'

export function usePermissions() {
  const { currentOrganization, currentProject } = useWorkspace()

  const can = (capability: Capability, projectId?: string): boolean => {
    // Check organization-level permission
    if (currentOrganization?.membership) {
      const hasOrgPermission = hasOrganizationCapability(currentOrganization.membership.role, capability)
      if (hasOrgPermission) return true
    }

    // Check project-level permission if projectId provided
    if (projectId && currentProject?.participation) {
      return hasProjectCapability(currentProject.participation.role, capability)
    }

    return false
  }

  const isOrganizationOwner = currentOrganization?.membership?.role === 'OWNER'
  const isOrganizationAdmin =
    currentOrganization?.membership?.role === 'OWNER' || currentOrganization?.membership?.role === 'ADMIN'

  return {
    can,
    isOrganizationOwner,
    isOrganizationAdmin,
  }
}
