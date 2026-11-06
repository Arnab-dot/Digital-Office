import { FileText, FolderKanban, Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { documentApi, projectApi } from '../../api'
import { Badge, Button, Card, EmptyState, LoadingSpinner } from '../../components/ui'
import type { Document, Project } from '../../types'

const statusColors: Record<string, 'info' | 'warning' | 'success' | 'error'> = {
  DRAFT: 'info',
  IN_REVIEW: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  ARCHIVED: 'info',
}

export function DocumentsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [selectedProjectId, setSelectedProjectId] = useState<string>('')
  const [documents, setDocuments] = useState<Document[]>([])
  const [loading, setLoading] = useState(true)
  const [docsLoading, setDocsLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    const loadProjects = async () => {
      setLoading(true)
      try {
        const response = await projectApi.list(0, 50)
        const projs = Array.isArray(response) ? response : []
        setProjects(projs)
        if (projs.length > 0) {
          setSelectedProjectId(projs[0].id)
        }
      } catch (err) {
        console.error('Failed to load projects:', err)
      } finally {
        setLoading(false)
      }
    }
    void loadProjects()
  }, [])

  useEffect(() => {
    if (!selectedProjectId) {
      setDocuments([])
      return
    }

    const loadDocuments = async () => {
      setDocsLoading(true)
      try {
        const response = await documentApi.list(selectedProjectId)
        setDocuments(response.content || [])
      } catch (error) {
        console.error('Failed to load documents:', error)
        setDocuments([])
      } finally {
        setDocsLoading(false)
      }
    }
    void loadDocuments()
  }, [selectedProjectId])

  if (loading) return <LoadingSpinner label="Loading documents..." />

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <div>
            <h1 className="page-header__title">Documents</h1>
            <p className="page-header__subtitle">
              Manage project documents, versions, and multi-party reviews
            </p>
          </div>
          {projects.length > 0 && (
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
              <select
                className="input"
                value={selectedProjectId}
                onChange={(e) => setSelectedProjectId(e.target.value)}
                style={{ padding: '8px 12px', borderRadius: '6px' }}
              >
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    Project: {p.name}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      </div>

      {projects.length === 0 ? (
        <EmptyState
          icon={FolderKanban}
          title="No projects found"
          description="Documents in Digital Office belong to projects. Create your first project to upload files, track version history, and conduct reviews."
          action={
            <Button variant="primary" onClick={() => navigate('/app/projects/new')}>
              <Plus size={16} /> Create Project First
            </Button>
          }
        />
      ) : docsLoading ? (
        <LoadingSpinner label="Loading project documents..." />
      ) : documents.length === 0 ? (
        <EmptyState
          icon={FileText}
          title="No documents in this project yet"
          description="Upload contracts, specifications, or deliverables to this project."
        />
      ) : (
        <div className="grid-list">
          {documents.map((doc) => (
            <Card key={doc.id} className="clickable-card" onClick={() => navigate(`/app/projects/${selectedProjectId}`)}>
              <div className="document-card">
                <div className="document-card__icon">
                  <FileText size={24} />
                </div>
                <div className="document-card__info">
                  <div className="document-card__header">
                    <h3>{doc.name}</h3>
                    <Badge variant={statusColors[doc.status] || 'info'}>
                      {doc.status.replace('_', ' ')}
                    </Badge>
                  </div>
                  {doc.description && <p>{doc.description}</p>}
                  <div className="document-card__meta">
                    <span>{(doc.sizeBytes / 1024).toFixed(1)} KB</span>
                    <span>{new Date(doc.updatedAt).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
