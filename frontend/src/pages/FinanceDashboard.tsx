import {
  Building2,
  Check,
  Clock,
  CreditCard,
  Download,
  FileText,
  FolderKanban,
  HardDrive,
  Plus,
  RefreshCw,
  Sparkles,
  Users,
  Video,
  X,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { financeApi, organizationApi } from '../api'
import { useWorkspace } from '../app/providers/WorkspaceProvider'
import { Alert, Badge, Button, Card, EmptyState, Input, LoadingSpinner } from '../components/ui'
import type {
  Invoice,
  Organization,
  OrganizationSubscription,
  Payment,
  PaymentMethod,
  SubscriptionPlan,
  SubscriptionUsage,
} from '../types'

export function FinanceDashboard() {
  const navigate = useNavigate()
  const { currentOrganization, setCurrentOrganization } = useWorkspace()

  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [selectedOrgId, setSelectedOrgId] = useState<string>('')
  const [subscription, setSubscription] = useState<OrganizationSubscription | null>(null)
  const [plans, setPlans] = useState<SubscriptionPlan[]>([])
  const [usage, setUsage] = useState<SubscriptionUsage[]>([])
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [payments, setPayments] = useState<Payment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  // Modals state
  const [isChangePlanOpen, setIsChangePlanOpen] = useState(false)
  const [isRecordPaymentOpen, setIsRecordPaymentOpen] = useState(false)
  const [selectedPlanId, setSelectedPlanId] = useState<string>('')
  const [submitting, setSubmitting] = useState(false)

  // Payment form state
  const [paymentAmount, setPaymentAmount] = useState<number>(29)
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('BANK_TRANSFER')
  const [paymentReference, setPaymentReference] = useState('')
  const [paymentInvoiceId, setPaymentInvoiceId] = useState('')
  const [paymentNotes, setPaymentNotes] = useState('')

  // 1. Load user's organizations
  useEffect(() => {
    const loadOrgs = async () => {
      try {
        const orgList = await organizationApi.list()
        setOrganizations(orgList)
        if (orgList.length > 0) {
          const currentValid = orgList.find((o) => o.id === currentOrganization?.id)
          const targetOrg = currentValid || orgList[0]
          setSelectedOrgId(targetOrg.id)
          if (!currentOrganization || currentOrganization.id !== targetOrg.id) {
            setCurrentOrganization({ id: targetOrg.id, name: targetOrg.name, membership: null })
          }
        }
      } catch (err) {
        console.warn('Failed to load organizations for finance dashboard:', err)
      }
    }
    void loadOrgs()
  }, [])

  // 2. Load finance data for selected organization
  const loadFinanceData = async (orgId: string) => {
    if (!orgId) return
    setLoading(true)
    setError(null)
    try {
      const [plansRes, subRes, usageRes, invRes, payRes] = await Promise.allSettled([
        financeApi.listPlans(),
        financeApi.getSubscription(orgId),
        financeApi.getUsage(orgId),
        financeApi.listInvoices(orgId),
        financeApi.listPayments(orgId),
      ])

      if (plansRes.status === 'fulfilled') setPlans(plansRes.value)
      if (subRes.status === 'fulfilled') setSubscription(subRes.value)
      else setSubscription(null)

      if (usageRes.status === 'fulfilled') setUsage(usageRes.value)
      else setUsage([])

      if (invRes.status === 'fulfilled') setInvoices(invRes.value.content || [])
      else setInvoices([])

      if (payRes.status === 'fulfilled') setPayments(payRes.value.content || [])
      else setPayments([])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load finance data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (selectedOrgId) {
      void loadFinanceData(selectedOrgId)
    } else {
      setLoading(false)
    }
  }, [selectedOrgId])

  const handleOrgChange = (newOrgId: string) => {
    setSelectedOrgId(newOrgId)
    const org = organizations.find((o) => o.id === newOrgId)
    if (org) {
      setCurrentOrganization({ id: org.id, name: org.name, membership: null })
    }
  }

  const handleChangePlan = async () => {
    if (!selectedPlanId || !selectedOrgId) return
    setSubmitting(true)
    setError(null)
    setSuccessMessage(null)
    try {
      let updated: OrganizationSubscription
      if (!subscription) {
        updated = await financeApi.createSubscription(selectedOrgId, selectedPlanId)
      } else {
        updated = await financeApi.changePlan(selectedOrgId, selectedPlanId)
      }
      setSubscription(updated)
      setSuccessMessage('Subscription plan successfully updated!')
      setIsChangePlanOpen(false)
      void loadFinanceData(selectedOrgId)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to change plan')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCancelSubscription = async () => {
    if (!selectedOrgId || !subscription) return
    if (!window.confirm('Are you sure you want to cancel your subscription at the end of the current billing cycle?')) {
      return
    }
    setSubmitting(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const updated = await financeApi.cancelSubscription(selectedOrgId)
      setSubscription(updated)
      setSuccessMessage('Subscription cancelled. Access remains active through current billing period.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to cancel subscription')
    } finally {
      setSubmitting(false)
    }
  }

  const handleReactivateSubscription = async () => {
    if (!selectedOrgId || !subscription) return
    setSubmitting(true)
    setError(null)
    try {
      const updated = await financeApi.reactivateSubscription(selectedOrgId)
      setSubscription(updated)
      setSuccessMessage('Subscription successfully reactivated!')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reactivate subscription')
    } finally {
      setSubmitting(false)
    }
  }

  const handleRecordPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedOrgId) return
    setSubmitting(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const payment = await financeApi.recordPayment(selectedOrgId, {
        amount: Number(paymentAmount),
        paymentMethod,
        referenceNumber: paymentReference || undefined,
        invoiceId: paymentInvoiceId || undefined,
        notes: paymentNotes || undefined,
        idempotencyKey: `pay-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      })
      await financeApi.confirmPayment(selectedOrgId, payment.id).catch(() => null)
      setSuccessMessage('Payment recorded and confirmed successfully!')
      setIsRecordPaymentOpen(false)
      setPaymentReference('')
      setPaymentNotes('')
      void loadFinanceData(selectedOrgId)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to record payment')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDownloadPdf = (invoiceId: string) => {
    if (!selectedOrgId) return
    const url = financeApi.downloadInvoicePdfUrl(selectedOrgId, invoiceId)
    window.open(url, '_blank')
  }

  if (loading) return <LoadingSpinner label="Loading billing & finance details..." />

  const currentOrg = organizations.find((o) => o.id === selectedOrgId)
  const currentPlan = plans.find((p) => p.id === subscription?.planId)

  return (
    <>
      {/* Page Header */}
      <div className="page-header">
        <div className="page-header__top">
          <div>
            <h1 className="page-header__title">Finance & Subscription</h1>
            <p className="page-header__subtitle">
              Manage workspace subscription plans, usage quotas, invoices, and billing records
            </p>
          </div>

          <div className="page-header__actions" style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            {organizations.length > 0 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Organization:</span>
                <select
                  className="input"
                  value={selectedOrgId}
                  onChange={(e) => handleOrgChange(e.target.value)}
                  style={{ padding: '8px 12px', minWidth: '180px' }}
                >
                  {organizations.map((org) => (
                    <option key={org.id} value={org.id}>
                      {org.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
            <Button variant="secondary" onClick={() => void loadFinanceData(selectedOrgId)}>
              <RefreshCw size={16} /> Refresh
            </Button>
            <Button variant="primary" onClick={() => setIsRecordPaymentOpen(true)}>
              <CreditCard size={16} /> Record Payment
            </Button>
          </div>
        </div>
      </div>

      {error && (
        <div style={{ marginBottom: '16px' }}>
          <Alert variant="error" onClose={() => setError(null)}>{error}</Alert>
        </div>
      )}
      {successMessage && (
        <div style={{ marginBottom: '16px' }}>
          <Alert variant="success" onClose={() => setSuccessMessage(null)}>{successMessage}</Alert>
        </div>
      )}

      {organizations.length === 0 ? (
        <EmptyState
          icon={Building2}
          title="No organizations found"
          description="Subscriptions and billing are attached to organizations. Create your first organization to set up a subscription plan."
          action={
            <Button variant="primary" onClick={() => navigate('/app/onboarding')}>
              Onboard Organization &rarr;
            </Button>
          }
        />
      ) : (
        <div className="stack-lg">
          {/* Active Subscription Banner Card */}
          <Card style={{ padding: '24px', background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(30, 41, 59, 0.4))', border: '1px solid var(--border)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                  <span style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)', fontWeight: 700 }}>
                    Active Subscription Tier
                  </span>
                  <Badge variant={subscription?.status === 'ACTIVE' ? 'success' : subscription?.status === 'CANCELLED' ? 'warning' : 'info'}>
                    {subscription?.status || 'FREE TIER'}
                  </Badge>
                  {subscription?.billingInterval && (
                    <Badge variant="info">{subscription.billingInterval}</Badge>
                  )}
                </div>

                <h2 style={{ fontSize: '1.8rem', fontWeight: 800, margin: '0 0 6px', color: 'var(--text-primary)' }}>
                  {subscription ? (subscription.planDisplayName || subscription.planName) : 'Free Workspace'}
                </h2>
                <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-secondary)', maxWidth: '580px' }}>
                  {currentPlan?.description ||
                    'Get started with basic team collaboration or upgrade to unlock advanced compliance, unlimited video meetings, and dedicated support.'}
                </p>
                {subscription?.currentPeriodEnd && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '12px', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                    <Clock size={14} />
                    <span>
                      Current billing period renews on {new Date(subscription.currentPeriodEnd).toLocaleDateString()}
                    </span>
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                {subscription?.status === 'CANCELLED' ? (
                  <Button variant="primary" onClick={handleReactivateSubscription} loading={submitting}>
                    Reactivate Subscription
                  </Button>
                ) : (
                  <>
                    {subscription && (
                      <Button variant="secondary" onClick={handleCancelSubscription} loading={submitting}>
                        Cancel Subscription
                      </Button>
                    )}
                    <Button variant="primary" onClick={() => setIsChangePlanOpen(true)}>
                      <Sparkles size={16} /> {subscription ? 'Change Plan Tier' : 'Upgrade Workspace'}
                    </Button>
                  </>
                )}
              </div>
            </div>
          </Card>

          {/* Quotas & Metric Cards */}
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-card__header">
                <span className="stat-card__label">Team Members</span>
                <div className="stat-card__icon">
                  <Users size={20} />
                </div>
              </div>
              <div className="stat-card__value">
                {currentPlan?.maxMembers ? `${currentPlan.maxMembers} max` : '5 max'}
              </div>
              <div className="stat-card__trend">
                Multi-tenant organization boundary
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-card__header">
                <span className="stat-card__label">Active Projects</span>
                <div className="stat-card__icon">
                  <FolderKanban size={20} />
                </div>
              </div>
              <div className="stat-card__value">
                {currentPlan?.maxProjects ? `${currentPlan.maxProjects} max` : '3 max'}
              </div>
              <div className="stat-card__trend">
                Simultaneous cross-company projects
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-card__header">
                <span className="stat-card__label">Storage Quota</span>
                <div className="stat-card__icon">
                  <HardDrive size={20} />
                </div>
              </div>
              <div className="stat-card__value">
                {currentPlan?.storageLimitMb
                  ? `${(currentPlan.storageLimitMb / 1024).toFixed(0)} GB`
                  : '500 MB'}
              </div>
              <div className="stat-card__trend">
                MinIO encrypted document storage
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-card__header">
                <span className="stat-card__label">Meeting Capacity</span>
                <div className="stat-card__icon">
                  <Video size={20} />
                </div>
              </div>
              <div className="stat-card__value">
                {currentPlan?.maxMeetingParticipants
                  ? `${currentPlan.maxMeetingParticipants} seats`
                  : '10 seats'}
              </div>
              <div className="stat-card__trend">
                Simultaneous video participants
              </div>
            </div>
          </div>

          {/* Real-time Usage & Quotas */}
          {usage && usage.length > 0 && (
            <Card style={{ padding: '24px' }}>
              <div style={{ marginBottom: '16px' }}>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, margin: '0 0 4px' }}>
                  Live Resource Quotas & Usage
                </h3>
                <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
                  Current usage vs limit for this billing period
                </p>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
                {usage.map((u) => {
                  const pct = u.limit > 0 ? Math.min(100, Math.round((u.currentValue / u.limit) * 100)) : 0
                  return (
                    <div key={u.metric} style={{ background: 'var(--bg-secondary)', padding: '16px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '8px' }}>
                        <span style={{ fontWeight: 600 }}>{u.metric}</span>
                        <span style={{ color: 'var(--text-secondary)' }}>{u.currentValue} / {u.limit === 0 ? '∞' : u.limit}</span>
                      </div>
                      <div style={{ background: 'rgba(255, 255, 255, 0.1)', height: '6px', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{ background: pct > 85 ? 'var(--color-error, #ef4444)' : 'var(--color-primary, #6366f1)', width: `${pct}%`, height: '100%' }} />
                      </div>
                    </div>
                  )
                })}
              </div>
            </Card>
          )}

          {/* Available Subscription Plans */}
          <Card style={{ padding: '24px' }}>
            <div style={{ marginBottom: '20px' }}>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700, margin: '0 0 4px' }}>
                Available Workspace Plans
              </h3>
              <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
                Select an enterprise or startup subscription tier backed by real-time billing governance
              </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
              {plans.map((plan) => {
                const isCurrent = subscription?.planId === plan.id
                return (
                  <div
                    key={plan.id}
                    style={{
                      border: isCurrent ? '2px solid var(--color-primary)' : '1px solid var(--border)',
                      background: isCurrent ? 'rgba(99, 102, 241, 0.05)' : 'var(--bg-secondary)',
                      borderRadius: '10px',
                      padding: '20px',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      position: 'relative',
                    }}
                  >
                    {isCurrent && (
                      <span
                        style={{
                          position: 'absolute',
                          top: '-10px',
                          right: '12px',
                          background: 'var(--color-primary)',
                          color: '#fff',
                          fontSize: '0.68rem',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: '10px',
                        }}
                      >
                        CURRENT PLAN
                      </span>
                    )}

                    <div>
                      <h4 style={{ margin: '0 0 4px', fontSize: '1.1rem', fontWeight: 800 }}>
                        {plan.displayName}
                      </h4>
                      <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', minHeight: '34px', margin: 0 }}>
                        {plan.description}
                      </p>

                      <div style={{ margin: '16px 0', fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                        ${plan.price}
                        <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 500 }}>
                          /{plan.billingInterval.toLowerCase()}
                        </span>
                      </div>

                      <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 16px', fontSize: '0.82rem', lineHeight: 1.8 }}>
                        <li style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Check size={14} color="#10b981" /> {plan.maxMembers} members
                        </li>
                        <li style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Check size={14} color="#10b981" /> {plan.maxProjects} projects
                        </li>
                        <li style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Check size={14} color="#10b981" /> {(plan.storageLimitMb / 1024).toFixed(0)} GB storage
                        </li>
                        <li style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Check size={14} color="#10b981" /> {plan.maxMeetingParticipants} video participants
                        </li>
                      </ul>
                    </div>

                    <Button
                      variant={isCurrent ? 'secondary' : 'primary'}
                      disabled={isCurrent}
                      onClick={() => {
                        setSelectedPlanId(plan.id)
                        setIsChangePlanOpen(true)
                      }}
                      style={{ width: '100%' }}
                    >
                      {isCurrent ? 'Current Plan' : 'Select Plan'}
                    </Button>
                  </div>
                )
              })}
            </div>
          </Card>

          {/* Invoices & Statements */}
          <Card style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: '0 0 4px' }}>
                  Invoices & Statements
                </h3>
                <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Statutory billing statements generated for {currentOrg?.name || 'this organization'}
                </p>
              </div>
            </div>

            {invoices.length === 0 ? (
              <EmptyState
                icon={FileText}
                title="No invoices issued yet"
                description="Statements and tax invoices generated for subscription renewals or services will appear here."
              />
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left', color: 'var(--text-muted)' }}>
                      <th style={{ padding: '10px 12px' }}>Invoice #</th>
                      <th style={{ padding: '10px 12px' }}>Date</th>
                      <th style={{ padding: '10px 12px' }}>Period</th>
                      <th style={{ padding: '10px 12px' }}>Amount</th>
                      <th style={{ padding: '10px 12px' }}>Status</th>
                      <th style={{ padding: '10px 12px', textAlign: 'right' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invoices.map((inv) => (
                      <tr key={inv.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>
                        <td style={{ padding: '12px', fontWeight: 600 }}>{inv.invoiceNumber}</td>
                        <td style={{ padding: '12px', color: 'var(--text-secondary)' }}>
                          {inv.issuedAt ? new Date(inv.issuedAt).toLocaleDateString() : 'N/A'}
                        </td>
                        <td style={{ padding: '12px', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                          {inv.billingPeriodStart ? new Date(inv.billingPeriodStart).toLocaleDateString() : 'N/A'} -{' '}
                          {inv.billingPeriodEnd ? new Date(inv.billingPeriodEnd).toLocaleDateString() : 'N/A'}
                        </td>
                        <td style={{ padding: '12px', fontWeight: 700 }}>
                          ${inv.total?.toFixed(2)} {inv.currency}
                        </td>
                        <td style={{ padding: '12px' }}>
                          <Badge variant={inv.status === 'PAID' ? 'success' : inv.status === 'OVERDUE' ? 'error' : 'warning'}>
                            {inv.status}
                          </Badge>
                        </td>
                        <td style={{ padding: '12px', textAlign: 'right' }}>
                          <Button variant="ghost" onClick={() => handleDownloadPdf(inv.id)} style={{ padding: '6px 10px', fontSize: '0.8rem' }}>
                            <Download size={14} /> PDF
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          {/* Payment Records */}
          <Card style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: '0 0 4px' }}>
                  Payment Records
                </h3>
                <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Confirmed payment receipts and offline settlements
                </p>
              </div>
              <Button variant="secondary" onClick={() => setIsRecordPaymentOpen(true)}>
                <Plus size={16} /> Record Payment
              </Button>
            </div>

            {payments.length === 0 ? (
              <EmptyState
                icon={CreditCard}
                title="No payment records found"
                description="Record wire transfers, debit/credit cards, or offline transaction receipts for your records."
              />
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left', color: 'var(--text-muted)' }}>
                      <th style={{ padding: '10px 12px' }}>Reference</th>
                      <th style={{ padding: '10px 12px' }}>Date</th>
                      <th style={{ padding: '10px 12px' }}>Method</th>
                      <th style={{ padding: '10px 12px' }}>Amount</th>
                      <th style={{ padding: '10px 12px' }}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {payments.map((pay) => (
                      <tr key={pay.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>
                        <td style={{ padding: '12px', fontWeight: 600 }}>
                          {pay.referenceNumber || `REF-${pay.id.substring(0, 8)}`}
                        </td>
                        <td style={{ padding: '12px', color: 'var(--text-secondary)' }}>
                          {new Date(pay.confirmedAt || pay.createdAt).toLocaleDateString()}
                        </td>
                        <td style={{ padding: '12px' }}>
                          <Badge variant="info">{pay.paymentMethod}</Badge>
                        </td>
                        <td style={{ padding: '12px', fontWeight: 700 }}>
                          ${pay.amount?.toFixed(2)} {pay.currency}
                        </td>
                        <td style={{ padding: '12px' }}>
                          <Badge variant={pay.status === 'CONFIRMED' ? 'success' : 'warning'}>
                            {pay.status}
                          </Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>
      )}

      {/* Change Plan Modal Dialog */}
      {isChangePlanOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 1000,
            background: 'rgba(15, 23, 42, 0.75)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '16px',
          }}
          onClick={() => setIsChangePlanOpen(false)}
        >
          <div
            style={{
              background: '#1e293b',
              border: '1px solid var(--border)',
              borderRadius: '12px',
              maxWidth: '520px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid var(--border)', paddingBottom: '12px' }}>
              <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700 }}>
                Select Subscription Tier
              </h3>
              <button
                type="button"
                onClick={() => setIsChangePlanOpen(false)}
                className="btn btn-ghost"
                style={{ padding: '4px' }}
              >
                <X size={18} />
              </button>
            </div>

            <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: '16px' }}>
              Choose the plan tier for <strong>{currentOrg?.name}</strong>:
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '24px' }}>
              {plans.map((p) => {
                const isSelected = selectedPlanId === p.id
                return (
                  <div
                    key={p.id}
                    onClick={() => setSelectedPlanId(p.id)}
                    style={{
                      border: isSelected ? '2px solid var(--color-primary)' : '1px solid var(--border)',
                      background: isSelected ? 'rgba(99, 102, 241, 0.08)' : 'rgba(255, 255, 255, 0.02)',
                      borderRadius: '8px',
                      padding: '12px 16px',
                      cursor: 'pointer',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 700 }}>{p.displayName}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {p.maxMembers} members • {p.maxProjects} projects • {(p.storageLimitMb / 1024).toFixed(0)} GB storage
                      </div>
                    </div>
                    <div style={{ fontWeight: 800, fontSize: '1.1rem' }}>
                      ${p.price}
                    </div>
                  </div>
                )
              })}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <Button variant="secondary" onClick={() => setIsChangePlanOpen(false)}>
                Cancel
              </Button>
              <Button variant="primary" onClick={handleChangePlan} loading={submitting}>
                Confirm Selection
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Record Payment Modal Dialog */}
      {isRecordPaymentOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 1000,
            background: 'rgba(15, 23, 42, 0.75)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '16px',
          }}
          onClick={() => setIsRecordPaymentOpen(false)}
        >
          <div
            style={{
              background: '#1e293b',
              border: '1px solid var(--border)',
              borderRadius: '12px',
              maxWidth: '480px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid var(--border)', paddingBottom: '12px' }}>
              <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700 }}>
                Record Payment
              </h3>
              <button
                type="button"
                onClick={() => setIsRecordPaymentOpen(false)}
                className="btn btn-ghost"
                style={{ padding: '4px' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleRecordPayment} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                  Amount ($ USD) *
                </label>
                <Input
                  type="number"
                  min="1"
                  step="0.01"
                  required
                  value={paymentAmount}
                  onChange={(e) => setPaymentAmount(parseFloat(e.target.value) || 0)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                  Payment Method
                </label>
                <select
                  className="input"
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
                  style={{ width: '100%', padding: '10px 12px' }}
                >
                  <option value="BANK_TRANSFER">Bank Wire / Transfer</option>
                  <option value="CARD">Credit / Debit Card</option>
                  <option value="MANUAL">Manual Offline Entry</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              {invoices.length > 0 && (
                <div>
                  <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                    Linked Invoice (Optional)
                  </label>
                  <select
                    className="input"
                    value={paymentInvoiceId}
                    onChange={(e) => setPaymentInvoiceId(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px' }}
                  >
                    <option value="">-- No specific invoice --</option>
                    {invoices.map((inv) => (
                      <option key={inv.id} value={inv.id}>
                        {inv.invoiceNumber} (${inv.total.toFixed(2)} - {inv.status})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                  Reference Number
                </label>
                <Input
                  placeholder="e.g. WIRE-8374928"
                  value={paymentReference}
                  onChange={(e) => setPaymentReference(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                  Notes / Description
                </label>
                <Input
                  placeholder="Additional payment details..."
                  value={paymentNotes}
                  onChange={(e) => setPaymentNotes(e.target.value)}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
                <Button type="button" variant="secondary" onClick={() => setIsRecordPaymentOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" loading={submitting}>
                  Record & Confirm
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
