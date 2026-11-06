import { CheckCircle2, Plus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { workApi } from '../../api'
import { Badge, Button, Card, EmptyState, Input, LoadingSpinner } from '../../components/ui'
import type { Task } from '../../types'

const statusColors: Record<string, 'info' | 'warning' | 'success' | 'error'> = {
  TODO: 'info',
  IN_PROGRESS: 'warning',
  IN_REVIEW: 'info',
  DONE: 'success',
  CANCELLED: 'error',
}

const priorityColors: Record<string, 'info' | 'warning' | 'error'> = {
  LOW: 'info',
  MEDIUM: 'info',
  HIGH: 'warning',
  URGENT: 'error',
}

export function TasksPage() {
  const [tasks, setTasks] = useState<Task[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const loadTasks = async () => {
      try {
        const response = await workApi.listTasks()
        const taskList = Array.isArray(response)
          ? response
          : Array.isArray((response as any)?.content)
          ? (response as any).content
          : []
        setTasks(taskList)
      } catch (error) {
        console.error('Failed to load tasks:', error)
        setTasks([])
      } finally {
        setLoading(false)
      }
    }
    void loadTasks()
  }, [])

  const filteredTasks = (tasks || []).filter((task) =>
    (task?.title || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
    (task?.description || '').toLowerCase().includes(searchQuery.toLowerCase())
  )

  if (loading) return <LoadingSpinner label="Loading tasks" />

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <h1 className="page-header__title">Tasks</h1>
          <Button variant="primary" onClick={() => navigate('/app/tasks/new')}>
            <Plus size={18} />
            New Task
          </Button>
        </div>
        <p className="page-header__subtitle">
          View and manage tasks across all projects
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
            placeholder="Search tasks..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '40px' }}
          />
        </div>
      </div>

      {filteredTasks.length === 0 ? (
        <EmptyState
          icon={CheckCircle2}
          title={searchQuery ? 'No tasks found' : 'No tasks yet'}
          description={searchQuery ? 'Try a different search term' : 'Create your first task to get started.'}
          action={
            !searchQuery && (
              <Button variant="primary" onClick={() => navigate('/app/tasks/new')}>
                New Task
              </Button>
            )
          }
        />
      ) : (
        <div className="task-list">
          {filteredTasks.map((task) => (
            <Card key={task.id} className="clickable-card" onClick={() => navigate(`/app/tasks/${task.id}`)}>
              <div className="task-card">
                <div className="task-card__header">
                  <div>
                    <h3>{task.title}</h3>
                    {task.description && <p className="task-card__description">{task.description}</p>}
                  </div>
                  <div className="task-card__badges">
                    <Badge variant={statusColors[task.status]}>{task.status.replace('_', ' ')}</Badge>
                    <Badge variant={priorityColors[task.priority]}>{task.priority}</Badge>
                  </div>
                </div>
                <div className="task-card__meta">
                  {task.assignee && <span>Assignee: {task.assignee.displayName}</span>}
                  {task.dueDate && (
                    <span>Due: {new Date(task.dueDate).toLocaleDateString()}</span>
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
