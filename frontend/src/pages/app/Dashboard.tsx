import {
  ArrowRight,
  Calendar,
  CheckCircle2,
  Clock,
  FileText,
  FolderKanban,
  Plus,
  Users,
  Video,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { activityApi, dashboardApi, meetingApi, projectApi } from '../../api'
import { useWorkspace } from '../../app/providers/WorkspaceProvider'
import { Button, Card, EmptyState, LoadingSpinner } from '../../components/ui'
import type { ActivityFeedItem, Meeting } from '../../types'

export function Dashboard() {
  const navigate = useNavigate()
  const { currentOrganization } = useWorkspace()

  const [loading, setLoading] = useState(true)
  const [activeProjectsCount, setActiveProjectsCount] = useState(0)
  const [pendingTasksCount, setPendingTasksCount] = useState(0)
  const [upcomingMeetingsCount, setUpcomingMeetingsCount] = useState(0)
  const [documentsForReviewCount, setDocumentsForReviewCount] = useState(0)
  const [recentActivities, setRecentActivities] = useState<ActivityFeedItem[]>([])
  const [warnings, setWarnings] = useState<string[]>([])

  useEffect(() => {
    let isMounted = true

    const loadDashboardData = async () => {
      setLoading(true)
      try {
        const [dashRes, projectsRes, meetingsRes] = await Promise.allSettled([
          dashboardApi.getSummary(),
          projectApi.list(0, 50),
          meetingApi.list(),
        ])

        if (!isMounted) return

        if (dashRes.status === 'fulfilled') {
          setPendingTasksCount(dashRes.value.activeTaskCount || 0)
          setDocumentsForReviewCount(dashRes.value.pendingReviews?.length || 0)
          if (dashRes.value.warnings?.length) {
            setWarnings(dashRes.value.warnings)
          }
        }

        if (projectsRes.status === 'fulfilled') {
          const projs = Array.isArray(projectsRes.value) ? projectsRes.value : []
          setActiveProjectsCount(projs.length)
        }

        if (meetingsRes.status === 'fulfilled') {
          const meetings = Array.isArray(meetingsRes.value) ? meetingsRes.value : []
          const upcoming = meetings.filter((m: Meeting) => m.status === 'ACTIVE')
          setUpcomingMeetingsCount(upcoming.length)
        }

        // Load real activity if org is present
        if (currentOrganization?.id) {
          try {
            const actRes = await activityApi.getOrgActivity(currentOrganization.id, undefined, 0, 5)
            if (isMounted) {
              setRecentActivities(actRes.content || [])
            }
          } catch {
            // activity optional
          }
        }
      } catch (err) {
        console.error('Error fetching real dashboard metrics:', err)
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    void loadDashboardData()

    return () => {
      isMounted = false
    }
  }, [currentOrganization?.id])

  if (loading) {
    return (
      <div className="app-content">
        <LoadingSpinner label="Loading live dashboard data..." />
      </div>
    )
  }

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <div>
            <h1 className="page-header__title">Dashboard</h1>
            <p className="page-header__subtitle">
              {currentOrganization
                ? `Workspace: ${currentOrganization.name} • Live overview`
                : 'Welcome back! Here is your live workspace status.'}
            </p>
          </div>
          <div className="page-header__actions" style={{ display: 'flex', gap: '8px' }}>
            <Button variant="secondary" onClick={() => navigate('/app/projects')}>
              View all projects
            </Button>
            <Button variant="primary" onClick={() => navigate('/app/meetings')}>
              <Video size={16} /> New meeting
            </Button>
          </div>
        </div>
      </div>

      {warnings.length > 0 && (
        <div style={{ marginBottom: '16px' }}>
          {warnings.map((w, idx) => (
            <div
              key={idx}
              style={{
                padding: '8px 12px',
                borderRadius: '6px',
                background: 'rgba(245, 158, 11, 0.1)',
                border: '1px solid rgba(245, 158, 11, 0.3)',
                color: '#fcd34d',
                fontSize: '0.85rem',
                marginBottom: '4px',
              }}
            >
              Notice: {w}
            </div>
          ))}
        </div>
      )}

      {/* Real Statistics Grid */}
      <div className="stats-grid">
        <div className="stat-card" onClick={() => navigate('/app/projects')} style={{ cursor: 'pointer' }}>
          <div className="stat-card__header">
            <span className="stat-card__label">Active Projects</span>
            <div className="stat-card__icon">
              <FolderKanban size={20} />
            </div>
          </div>
          <div className="stat-card__value">{activeProjectsCount}</div>
          <div className="stat-card__trend">
            {activeProjectsCount === 0 ? 'No projects created yet' : 'In your active workspace'}
          </div>
        </div>

        <div className="stat-card" onClick={() => navigate('/app/tasks')} style={{ cursor: 'pointer' }}>
          <div className="stat-card__header">
            <span className="stat-card__label">Pending Tasks</span>
            <div className="stat-card__icon">
              <CheckCircle2 size={20} />
            </div>
          </div>
          <div className="stat-card__value">{pendingTasksCount}</div>
          <div className="stat-card__trend">
            {pendingTasksCount === 0 ? 'All caught up' : 'Assigned across projects'}
          </div>
        </div>

        <div className="stat-card" onClick={() => navigate('/app/meetings')} style={{ cursor: 'pointer' }}>
          <div className="stat-card__header">
            <span className="stat-card__label">Upcoming Meetings</span>
            <div className="stat-card__icon">
              <Calendar size={20} />
            </div>
          </div>
          <div className="stat-card__value">{upcomingMeetingsCount}</div>
          <div className="stat-card__trend">
            {upcomingMeetingsCount === 0 ? 'No scheduled meetings' : 'Next 7 days'}
          </div>
        </div>

        <div className="stat-card" onClick={() => navigate('/app/documents')} style={{ cursor: 'pointer' }}>
          <div className="stat-card__header">
            <span className="stat-card__label">Documents for Review</span>
            <div className="stat-card__icon">
              <FileText size={20} />
            </div>
          </div>
          <div className="stat-card__value">{documentsForReviewCount}</div>
          <div className="stat-card__trend">
            {documentsForReviewCount === 0 ? 'No pending reviews' : 'Awaiting sign-off'}
          </div>
        </div>
      </div>

      {/* Getting Started Callout if 0 Projects */}
      {activeProjectsCount === 0 && (
        <Card style={{ marginBottom: 'var(--space-lg)', border: '1px dashed var(--border)', background: 'rgba(99, 102, 241, 0.03)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h3 style={{ margin: '0 0 6px', fontSize: '1.05rem', fontWeight: 700 }}>
                🚀 Ready to start your first project?
              </h3>
              <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
                You currently have no active projects. Create a project to assign tasks, invite collaborators, and manage document approvals.
              </p>
            </div>
            <Button variant="primary" onClick={() => navigate('/app/projects/new')}>
              <Plus size={16} /> Create Project
            </Button>
          </div>
        </Card>
      )}

      <div className="dashboard-sections">
        {/* Recent Activity */}
        <Card>
          <div className="card-header">
            <div>
              <h3 className="card-title">Recent Activity</h3>
              <p className="card-subtitle">Live events from your projects and teams</p>
            </div>
            {recentActivities.length > 0 && (
              <Button variant="ghost" onClick={() => navigate('/app/activity')}>
                View all <ArrowRight size={16} />
              </Button>
            )}
          </div>

          {recentActivities.length > 0 ? (
            <div className="activity-list">
              {recentActivities.map((item) => (
                <div key={item.id} className="activity-item">
                  <div className="activity-item__icon">
                    <CheckCircle2 size={16} />
                  </div>
                  <div className="activity-item__content">
                    <p className="activity-item__description">{item.description}</p>
                    <span className="activity-item__timestamp">
                      {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={Clock}
              title="No recent activity"
              description="Real events from your projects, tasks, and meetings will automatically appear here."
            />
          )}
        </Card>

        {/* Quick Actions */}
        <Card>
          <div className="card-header">
            <div>
              <h3 className="card-title">Quick Actions</h3>
              <p className="card-subtitle">Common workspace actions</p>
            </div>
          </div>

          <div className="quick-actions">
            <button
              type="button"
              className="quick-action"
              onClick={() => navigate('/app/projects/new')}
            >
              <FolderKanban size={20} />
              <span>Create Project</span>
            </button>
            <button
              type="button"
              className="quick-action"
              onClick={() => navigate('/app/clients/new')}
            >
              <Users size={20} />
              <span>Add Client</span>
            </button>
            <button
              type="button"
              className="quick-action"
              onClick={() => navigate('/app/documents')}
            >
              <FileText size={20} />
              <span>Documents</span>
            </button>
            <button
              type="button"
              className="quick-action"
              onClick={() => navigate('/app/meetings')}
            >
              <Calendar size={20} />
              <span>Schedule Meeting</span>
            </button>
          </div>
        </Card>
      </div>
    </>
  )
}
