import { ArrowLeft, Calendar, DollarSign, FolderKanban } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { projectApi } from '../../api'
import { Badge, Button, Card, LoadingSpinner } from '../../components/ui'
import type { Project } from '../../types'

const statusColors: Record<string, 'success' | 'info' | 'warning' | 'error'> = {
  ACTIVE: 'success',
  PLANNING: 'info',
  ON_HOLD: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'error',
}

export function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [project, setProject] = useState<Project | null>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    if (!id) return

    const loadProject = async () => {
      try {
        const data = await projectApi.get(id)
        setProject(data)
      } catch (error) {
        console.error('Failed to load project:', error)
      } finally {
        setLoading(false)
      }
    }
    void loadProject()
  }, [id])

  if (loading) return <LoadingSpinner label="Loading project" />
  if (!project) return <div>Project not found</div>

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <Button variant="ghost" onClick={() => navigate('/app/projects')}>
            <ArrowLeft size={18} />
            Back to Projects
          </Button>
          <div className="page-header__actions">
            <Button variant="secondary">Edit Project</Button>
            <Button variant="secondary">Invite Organization</Button>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gap: 'var(--space-lg)' }}>
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 'var(--space-lg)' }}>
            <div>
              <h1 style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 'var(--font-weight-bold)', marginBottom: 'var(--space-xs)' }}>
                {project.name}
              </h1>
              <Badge variant={statusColors[project.status] || 'default'}>
                {project.status.replace('_', ' ')}
              </Badge>
            </div>
            <div className="project-icon">
              <FolderKanban size={32} />
            </div>
          </div>

          {project.description && (
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: 'var(--space-lg)' }}>
              {project.description}
            </p>
          )}

          <div className="detail-grid">
            {project.startDate && (
              <div className="detail-item">
                <Calendar size={18} />
                <div>
                  <div className="detail-label">Start Date</div>
                  <div className="detail-value">{new Date(project.startDate).toLocaleDateString()}</div>
                </div>
              </div>
            )}

            {project.targetEndDate && (
              <div className="detail-item">
                <Calendar size={18} />
                <div>
                  <div className="detail-label">Target End Date</div>
                  <div className="detail-value">{new Date(project.targetEndDate).toLocaleDateString()}</div>
                </div>
              </div>
            )}

            {project.budget && (
              <div className="detail-item">
                <DollarSign size={18} />
                <div>
                  <div className="detail-label">Budget</div>
                  <div className="detail-value">
                    {project.currency} {project.budget.toLocaleString()}
                  </div>
                </div>
              </div>
            )}
          </div>
        </Card>

        <Card>
          <h2 style={{ fontSize: 'var(--font-size-xl)', fontWeight: 'var(--font-weight-bold)', marginBottom: 'var(--space-md)' }}>
            Quick Actions
          </h2>
          <div className="quick-actions">
            <button type="button" className="quick-action" onClick={() => navigate(`/app/projects/${id}/tasks`)}>
              View Tasks
            </button>
            <button type="button" className="quick-action" onClick={() => navigate(`/app/projects/${id}/documents`)}>
              View Documents
            </button>
            <button type="button" className="quick-action" onClick={() => navigate(`/app/projects/${id}/participants`)}>
              View Participants
            </button>
            <button type="button" className="quick-action" onClick={() => navigate(`/app/projects/${id}/activity`)}>
              View Activity
            </button>
          </div>
        </Card>
      </div>
    </>
  )
}
