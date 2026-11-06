import { FolderKanban, Plus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { projectApi } from '../../api'
import { Badge, Button, Card, EmptyState, Input, LoadingSpinner } from '../../components/ui'
import type { Project } from '../../types'

const statusColors: Record<string, 'success' | 'info' | 'warning' | 'error'> = {
  ACTIVE: 'success',
  PLANNING: 'info',
  ON_HOLD: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'error',
}

export function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const loadProjects = async () => {
      try {
        const response = await projectApi.list()
        const projectList = Array.isArray(response)
          ? response
          : Array.isArray((response as any)?.content)
          ? (response as any).content
          : []
        setProjects(projectList)
      } catch (error) {
        console.error('Failed to load projects:', error)
        setProjects([])
      } finally {
        setLoading(false)
      }
    }
    void loadProjects()
  }, [])

  const filteredProjects = (projects || []).filter((project) =>
    (project?.name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
    (project?.description || '').toLowerCase().includes(searchQuery.toLowerCase())
  )

  if (loading) return <LoadingSpinner label="Loading projects" />

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <h1 className="page-header__title">Projects</h1>
          <Button variant="primary" onClick={() => navigate('/app/projects/new')}>
            <Plus size={18} />
            New Project
          </Button>
        </div>
        <p className="page-header__subtitle">
          Manage projects across your organizations and clients
        </p>
      </div>

      <div style={{ marginBottom: 'var(--space-lg)' }}>
        <div style={{ position: 'relative', maxWidth: '400px' }}>
          <Search
            size={18}
            style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}
          />
          <Input
            type="search"
            placeholder="Search projects..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '40px' }}
          />
        </div>
      </div>

      {filteredProjects.length === 0 ? (
        <EmptyState
          icon={FolderKanban}
          title={searchQuery ? 'No projects found' : 'No projects yet'}
          description={searchQuery ? 'Try a different search term' : 'Create your first project to get started.'}
          action={
            !searchQuery && (
              <Button variant="primary" onClick={() => navigate('/app/projects/new')}>
                New Project
              </Button>
            )
          }
        />
      ) : (
        <div className="grid-list">
          {filteredProjects.map((project) => (
            <Card key={project.id} className="clickable-card" onClick={() => navigate(`/app/projects/${project.id}`)}>
              <div className="project-card">
                <div className="project-card__header">
                  <h3>{project.name}</h3>
                  <Badge variant={statusColors[project.status] || 'default'}>
                    {project.status.replace('_', ' ')}
                  </Badge>
                </div>
                {project.description && <p className="project-card__description">{project.description}</p>}
                <div className="project-card__meta">
                  {project.startDate && (
                    <span>Started: {new Date(project.startDate).toLocaleDateString()}</span>
                  )}
                  {project.budget && (
                    <span>Budget: {project.currency} {project.budget.toLocaleString()}</span>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
