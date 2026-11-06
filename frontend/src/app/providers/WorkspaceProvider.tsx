import { createContext, useContext, useState, type ReactNode } from 'react'
import type { OrganizationMembership, ProjectParticipation } from '../../lib/permissions/types'

interface Organization {
  id: string
  name: string
  membership: OrganizationMembership | null
}

interface Project {
  id: string
  name: string
  organizationId: string
  participation: ProjectParticipation | null
}

interface WorkspaceContextValue {
  currentOrganization: Organization | null
  setCurrentOrganization: (org: Organization | null) => void
  currentProject: Project | null
  setCurrentProject: (project: Project | null) => void
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null)

export function useWorkspace() {
  const context = useContext(WorkspaceContext)
  if (!context) {
    throw new Error('useWorkspace must be used within WorkspaceProvider')
  }
  return context
}

interface WorkspaceProviderProps {
  children: ReactNode
}

export function WorkspaceProvider({ children }: WorkspaceProviderProps) {
  const [currentOrganization, setCurrentOrganization] = useState<Organization | null>(null)
  const [currentProject, setCurrentProject] = useState<Project | null>(null)

  const value: WorkspaceContextValue = {
    currentOrganization,
    setCurrentOrganization,
    currentProject,
    setCurrentProject,
  }

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
}
