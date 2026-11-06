import { ArrowLeft } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { projectApi } from '../../api'
import { Alert, Button, Card, Input, Select, TextArea } from '../../components/ui'

export function CreateProjectPage() {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState('PLANNING')
  const [startDate, setStartDate] = useState('')
  const [targetEndDate, setTargetEndDate] = useState('')
  const [budget, setBudget] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const project = await projectApi.create({
        name,
        description: description || undefined,
        status: status as any,
        startDate: startDate || undefined,
        targetEndDate: targetEndDate || undefined,
        budget: budget ? parseFloat(budget) : undefined,
        currency: budget ? currency : undefined,
      })

      navigate(`/app/projects/${project.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create project')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <Button variant="ghost" onClick={() => navigate('/app/projects')}>
            <ArrowLeft size={18} />
            Back to Projects
          </Button>
        </div>
        <h1 className="page-header__title">Create New Project</h1>
        <p className="page-header__subtitle">
          Set up a new project to organize work and collaborate with your team
        </p>
      </div>

      <Card style={{ maxWidth: '800px' }}>
        {error && <Alert variant="error" onClose={() => setError(null)}>{error}</Alert>}

        <form onSubmit={handleSubmit} className="stack-lg">
          <Input
            label="Project Name"
            placeholder="Q4 Marketing Campaign"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            autoFocus
          />

          <TextArea
            label="Description"
            placeholder="Describe the project goals and scope..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={4}
          />

          <Select
            label="Status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            options={[
              { value: 'PLANNING', label: 'Planning' },
              { value: 'ACTIVE', label: 'Active' },
              { value: 'ON_HOLD', label: 'On Hold' },
              { value: 'COMPLETED', label: 'Completed' },
            ]}
          />

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
            <Input
              type="date"
              label="Start Date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />

            <Input
              type="date"
              label="Target End Date"
              value={targetEndDate}
              onChange={(e) => setTargetEndDate(e.target.value)}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 'var(--space-md)' }}>
            <Input
              type="number"
              label="Budget"
              placeholder="10000"
              value={budget}
              onChange={(e) => setBudget(e.target.value)}
              step="0.01"
            />

            <Select
              label="Currency"
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              options={[
                { value: 'USD', label: 'USD' },
                { value: 'EUR', label: 'EUR' },
                { value: 'GBP', label: 'GBP' },
                { value: 'INR', label: 'INR' },
              ]}
            />
          </div>

          <div style={{ display: 'flex', gap: 'var(--space-md)', justifyContent: 'flex-end', marginTop: 'var(--space-lg)' }}>
            <Button type="button" variant="secondary" onClick={() => navigate('/app/projects')}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Create Project
            </Button>
          </div>
        </form>
      </Card>
    </>
  )
}
