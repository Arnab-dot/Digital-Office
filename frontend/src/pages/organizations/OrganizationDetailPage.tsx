import {
  ArrowLeft,
  Briefcase,
  Building2,
  CheckCircle2,
  DollarSign,
  FileCheck,
  FileText,
  FolderKanban,
  Mail,
  Plus,
  Rocket,
  Send,
  Shield,
  ShieldCheck,
  Trash2,
  UserCheck,
  Users,
  Video,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { financeApi, organizationApi, projectApi } from '../../api'
import { useWorkspace } from '../../app/providers/WorkspaceProvider'
import { Alert, Badge, Button, Card, Input, LoadingSpinner } from '../../components/ui'
import type {
  Invitation,
  Organization,
  OrganizationMembership,
  OrganizationProfile,
  OrganizationRole,
  OrganizationSubscription,
  Project,
  SentEmail,
} from '../../types'

export function OrganizationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { setCurrentOrganization } = useWorkspace()

  const [org, setOrg] = useState<Organization | null>(null)
  const [profile, setProfile] = useState<OrganizationProfile | null>(null)
  const [members, setMembers] = useState<OrganizationMembership[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [sentEmails, setSentEmails] = useState<SentEmail[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [subscription, setSubscription] = useState<OrganizationSubscription | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Invite state
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<OrganizationRole>('MEMBER')
  const [inviteLoading, setInviteLoading] = useState(false)
  const [inviteMessage, setInviteMessage] = useState<string | null>(null)

  // Domain Policy state
  const [domainPolicyEnforce, setDomainPolicyEnforce] = useState(false)
  const [domainPolicyAllowed, setDomainPolicyAllowed] = useState('')
  const [domainSaving, setDomainSaving] = useState(false)

  useEffect(() => {
    if (!id) return

    const loadData = async () => {
      setLoading(true)
      setError(null)
      try {
        const orgData = await organizationApi.get(id)
        setOrg(orgData)
        setCurrentOrganization({ id: orgData.id, name: orgData.name, membership: null })

        // Load members, projects, subscription, profile, invitations, emails in parallel
        const [membersData, projectsData, subData, profileData, invData, emailsData] = await Promise.allSettled([
          organizationApi.listMembers(id),
          projectApi.list(0, 20),
          financeApi.getSubscription(id),
          organizationApi.getProfile(id),
          organizationApi.listInvitations(id),
          organizationApi.listSentEmails(id),
        ])

        if (membersData.status === 'fulfilled') {
          setMembers(membersData.value)
        }
        if (projectsData.status === 'fulfilled') {
          const allProjects: Project[] = Array.isArray(projectsData.value) ? projectsData.value : []
          setProjects(allProjects.filter((p: Project) => p.ownerOrganizationId === id))
        }
        if (subData.status === 'fulfilled') {
          setSubscription(subData.value)
        }
        if (profileData.status === 'fulfilled') {
          setProfile(profileData.value)
          setDomainPolicyEnforce(Boolean(profileData.value?.enforceCompanyDomain))
          setDomainPolicyAllowed(profileData.value?.allowedEmailDomain || '')
        }
        if (invData.status === 'fulfilled') {
          setInvitations(invData.value.filter((i) => i.status === 'PENDING'))
        }
        if (emailsData.status === 'fulfilled') {
          setSentEmails(emailsData.value)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load organization details')
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [id, setCurrentOrganization])

  const handleSendInvite = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id || !inviteEmail) return
    setInviteLoading(true)
    setInviteMessage(null)
    setError(null)

    try {
      await organizationApi.createInvitation(id, inviteEmail, inviteRole)
      setInviteMessage(`Invitation email dispatched successfully to ${inviteEmail}!`)
      setInviteEmail('')

      const [newInvs, newEmails] = await Promise.all([
        organizationApi.listInvitations(id),
        organizationApi.listSentEmails(id),
      ])
      setInvitations(newInvs.filter((i) => i.status === 'PENDING'))
      setSentEmails(newEmails)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send invitation')
    } finally {
      setInviteLoading(false)
    }
  }

  const handleRevokeInvite = async (invitationId: string) => {
    if (!id) return
    try {
      await organizationApi.revokeInvitation(id, invitationId)
      setInvitations((prev) => prev.filter((i) => i.id !== invitationId))
      setInviteMessage('Invitation revoked.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to revoke invitation')
    }
  }

  const handleSetWorkspace = () => {
    if (org) {
      setCurrentOrganization({ id: org.id, name: org.name, membership: null })
      navigate('/app')
    }
  }

  const handleSaveDomainPolicy = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id) return
    setDomainSaving(true)
    setError(null)
    try {
      const updated = await organizationApi.updateProfile(id, {
        enforceCompanyDomain: domainPolicyEnforce,
        allowedEmailDomain: domainPolicyAllowed.trim() || null,
      })
      setProfile(updated)
      setInviteMessage('Email domain policy updated successfully.')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to update domain policy')
    } finally {
      setDomainSaving(false)
    }
  }

  if (loading) return <LoadingSpinner label="Loading organization details" />
  if (!org) {
    return (
      <div className="stack-lg">
        <Button variant="ghost" onClick={() => navigate('/app/organizations')}>
          <ArrowLeft size={18} /> Back to Organizations
        </Button>
        <Alert variant="error">{error || 'Organization not found'}</Alert>
      </div>
    )
  }

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <Button variant="ghost" onClick={() => navigate('/app/organizations')}>
            <ArrowLeft size={18} />
            Back to Organizations
          </Button>
          <div className="page-header__actions" style={{ display: 'flex', gap: '8px' }}>
            <Button variant="secondary" onClick={() => navigate('/app/projects/new')}>
              <Plus size={16} /> New Project
            </Button>
            <Button variant="primary" onClick={handleSetWorkspace}>
              <CheckCircle2 size={16} /> Open Workspace
            </Button>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginTop: '8px' }}>
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '10px',
              background: 'linear-gradient(135deg, #3b82f6, #6366f1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#fff',
              fontSize: '1.4rem',
              fontWeight: 700,
            }}
          >
            {org.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <h1 className="page-header__title" style={{ margin: 0 }}>
              {org.name}
            </h1>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '4px' }}>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>@{org.slug || 'workspace'}</span>
              <Badge variant={org.status === 'ACTIVE' || org.active ? 'success' : 'warning'}>
                {org.status || (org.active ? 'ACTIVE' : 'INACTIVE')}
              </Badge>
              {profile && (
                <Badge variant={profile.entityType === 'CORPORATE' ? 'info' : 'success'}>
                  {profile.entityType === 'CORPORATE' ? <Building2 size={12} style={{ marginRight: '4px' }} /> : <Rocket size={12} style={{ marginRight: '4px' }} />}
                  {profile.entityType}
                </Badge>
              )}
              {profile && (
                <Badge variant={profile.verificationStatus === 'VERIFIED' ? 'success' : 'warning'}>
                  <ShieldCheck size={12} style={{ marginRight: '4px' }} />
                  {profile.verificationStatus === 'VERIFIED' ? 'VERIFIED ENTITY' : 'KYC PENDING'}
                </Badge>
              )}
              {subscription && (
                <Badge variant="info">
                  {subscription.planDisplayName || subscription.planName.toUpperCase()} PLAN
                </Badge>
              )}
            </div>
          </div>
        </div>
      </div>

      {error && <Alert variant="error" onClose={() => setError(null)}>{error}</Alert>}
      {inviteMessage && <Alert variant="success" onClose={() => setInviteMessage(null)}>{inviteMessage}</Alert>}

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 'var(--space-lg)' }}>
        {/* Left Column: Projects & Members */}
        <div className="stack-lg">
          {/* Corporate / Startup KYC Profile */}
          {profile ? (
            <Card>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: 'var(--space-md)',
                  borderBottom: '1px solid var(--border)',
                  paddingBottom: '12px',
                }}
              >
                <h2 style={{ fontSize: '1.1rem', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <ShieldCheck size={18} color="var(--color-primary)" /> Legal Registration & Compliance
                </h2>
                <Badge variant={profile.verificationStatus === 'VERIFIED' ? 'success' : 'warning'}>
                  {profile.verificationStatus}
                </Badge>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', fontSize: '0.88rem' }}>
                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem', display: 'block' }}>Legal Registered Name</span>
                  <strong>{profile.legalName}</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem', display: 'block' }}>Registration # (CIN/EIN)</span>
                  <strong>{profile.registrationNumber || 'Not provided'}</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem', display: 'block' }}>Tax ID (GSTIN/VAT/TIN)</span>
                  <strong>{profile.taxId || 'Not provided'}</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem', display: 'block' }}>Jurisdiction & Country</span>
                  <strong>{profile.jurisdiction || 'General'}, {profile.country || 'N/A'}</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem', display: 'block' }}>Industry & Stage</span>
                  <strong>{profile.industry || 'Tech'} ({profile.stage || 'Active'})</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem', display: 'block' }}>Authorized Officer</span>
                  <strong>{profile.signatoryName || 'N/A'} {profile.signatoryTitle ? `(${profile.signatoryTitle})` : ''}</strong>
                </div>
              </div>

              {/* Uploaded Documents */}
              <div style={{ marginTop: '16px', borderTop: '1px solid var(--border)', paddingTop: '12px' }}>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem', display: 'block', marginBottom: '8px' }}>
                  Compliance Verification Documents
                </span>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {profile.incorporationDocName ? (
                    <Badge variant="success">
                      <FileCheck size={13} style={{ marginRight: '4px' }} /> {profile.incorporationDocName}
                    </Badge>
                  ) : (
                    <Badge variant="warning">Certificate of Incorporation missing</Badge>
                  )}
                  {profile.taxDocName && (
                    <Badge variant="info">
                      <FileText size={13} style={{ marginRight: '4px' }} /> {profile.taxDocName}
                    </Badge>
                  )}
                  {profile.pitchDeckDocName && (
                    <Badge variant="info">
                      <Briefcase size={13} style={{ marginRight: '4px' }} /> {profile.pitchDeckDocName}
                    </Badge>
                  )}
                </div>
              </div>

              {/* Domain Restriction & Email Policy */}
              <div style={{ marginTop: '16px', borderTop: '1px solid var(--border)', paddingTop: '12px' }}>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem', display: 'block', marginBottom: '8px' }}>
                  Email Invitation & Corporate Domain Policy
                </span>
                <form onSubmit={handleSaveDomainPolicy} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.85rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={domainPolicyEnforce}
                      onChange={(e) => setDomainPolicyEnforce(e.target.checked)}
                    />
                    <span>Enforce company domain restrictions on employee invitations</span>
                  </label>

                  {domainPolicyEnforce && (
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                      <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>@</span>
                      <Input
                        placeholder="e.g. yourcompany.com"
                        value={domainPolicyAllowed}
                        onChange={(e) => setDomainPolicyAllowed(e.target.value)}
                        required={domainPolicyEnforce}
                        style={{ maxWidth: '280px' }}
                      />
                    </div>
                  )}

                  <div>
                    <Button type="submit" variant="secondary" loading={domainSaving} style={{ padding: '6px 14px', fontSize: '0.82rem' }}>
                      Save Domain Policy
                    </Button>
                  </div>
                </form>
              </div>
            </Card>
          ) : (
            <Card style={{ border: '1px dashed var(--border)', background: 'rgba(99, 102, 241, 0.03)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3 style={{ margin: '0 0 4px', fontSize: '1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <ShieldCheck size={18} color="var(--color-primary)" /> Complete Corporate & Startup Onboarding
                  </h3>
                  <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    Add CIN/EIN registration numbers, tax identifiers, and upload incorporation documents.
                  </p>
                </div>
                <Button variant="primary" onClick={() => navigate('/app/onboarding')}>
                  Onboard Now &rarr;
                </Button>
              </div>
            </Card>
          )}

          {/* Projects in this Org */}
          <Card>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 'var(--space-md)',
              }}
            >
              <h2 style={{ fontSize: '1.1rem', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <FolderKanban size={18} /> Projects ({projects.length})
              </h2>
              <Button variant="ghost" onClick={() => navigate('/app/projects/new')}>
                <Plus size={16} /> Add Project
              </Button>
            </div>

            {projects.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                No projects created in this organization yet.{' '}
                <Link to="/app/projects/new" style={{ color: 'var(--primary)' }}>
                  Create the first project &rarr;
                </Link>
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {projects.map((proj) => (
                  <div
                    key={proj.id}
                    onClick={() => navigate(`/app/projects/${proj.id}`)}
                    style={{
                      padding: '12px 16px',
                      borderRadius: '8px',
                      background: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid var(--border)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      cursor: 'pointer',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 600 }}>{proj.name}</div>
                      {proj.description && (
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                          {proj.description}
                        </div>
                      )}
                    </div>
                    <Badge variant={proj.status === 'ACTIVE' ? 'success' : 'info'}>{proj.status}</Badge>
                  </div>
                ))}
              </div>
            )}
          </Card>

          {/* Members List */}
          <Card>
            <h2 style={{ fontSize: '1.1rem', margin: '0 0 var(--space-md)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Users size={18} /> Team Members ({members.length})
            </h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {members.map((m) => (
                <div
                  key={m.id}
                  style={{
                    padding: '10px 14px',
                    borderRadius: '8px',
                    background: 'rgba(255, 255, 255, 0.02)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    border: '1px solid var(--border)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <div
                      style={{
                        width: '32px',
                        height: '32px',
                        borderRadius: '50%',
                        background: '#334155',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '0.85rem',
                        fontWeight: 600,
                      }}
                    >
                      <UserCheck size={16} />
                    </div>
                    <div>
                      <div style={{ fontSize: '0.9rem', fontWeight: 500 }}>
                        User {m.userId.substring(0, 8)}...
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        Status: {m.status}
                      </div>
                    </div>
                  </div>
                  <Badge variant={m.role === 'OWNER' ? 'success' : m.role === 'ADMIN' ? 'warning' : 'info'}>
                    {m.role}
                  </Badge>
                </div>
              ))}
            </div>
          </Card>
        </div>

        {/* Right Column: Invite & Quick Tools */}
        <div className="stack-lg">
          {/* Invite Member */}
          <Card>
            <h3 style={{ fontSize: '1rem', margin: '0 0 var(--space-sm)', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Mail size={16} /> Invite Member
            </h3>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: '8px' }}>
              Invite a collaborator or colleague to join {org.name}.
            </p>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '14px' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Mailing Policy:</span>
              {profile?.enforceCompanyDomain && profile?.allowedEmailDomain ? (
                <Badge variant="warning">@{profile.allowedEmailDomain} only</Badge>
              ) : (
                <Badge variant="info">Open • Any mail service</Badge>
              )}
            </div>

            <form onSubmit={handleSendInvite} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <Input
                type="email"
                placeholder={profile?.enforceCompanyDomain && profile?.allowedEmailDomain ? `colleague@${profile.allowedEmailDomain}` : 'colleague@gmail.com'}
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                required
              />

              <div style={{ display: 'flex', gap: '8px' }}>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value as OrganizationRole)}
                  className="input"
                  style={{ flex: 1, padding: '8px 12px', fontSize: '0.85rem' }}
                >
                  <option value="MEMBER">Member</option>
                  <option value="ADMIN">Admin</option>
                </select>
                <Button type="submit" variant="primary" loading={inviteLoading} style={{ whiteSpace: 'nowrap' }}>
                  <Send size={15} /> Send Invite
                </Button>
              </div>
            </form>

            {/* Pending Invitations */}
            {invitations.length > 0 && (
              <div style={{ marginTop: '20px', borderTop: '1px solid var(--border)', paddingTop: '14px' }}>
                <h4 style={{ fontSize: '0.88rem', margin: '0 0 10px', color: 'var(--text-primary)' }}>
                  Pending Invitations ({invitations.length})
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {invitations.map((inv) => (
                    <div
                      key={inv.id}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        padding: '8px 10px',
                        background: 'rgba(255, 255, 255, 0.02)',
                        border: '1px solid var(--border)',
                        borderRadius: '6px',
                        fontSize: '0.82rem',
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 600 }}>{inv.inviteeEmail}</div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Role: {inv.intendedRole}</div>
                      </div>
                      <Button
                        variant="ghost"
                        onClick={() => handleRevokeInvite(inv.id)}
                        title="Revoke Invitation"
                        style={{ padding: '4px' }}
                      >
                        <Trash2 size={14} color="#ef4444" />
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Card>

          {/* Outgoing Emails Log */}
          {sentEmails.length > 0 && (
            <Card>
              <h3 style={{ fontSize: '0.95rem', margin: '0 0 var(--space-sm)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Send size={15} /> Outgoing Email Dispatch ({sentEmails.length})
              </h3>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: '0 0 10px' }}>
                Dispatched via internal mail service.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {sentEmails.slice(0, 5).map((email) => (
                  <div
                    key={email.id}
                    style={{
                      padding: '8px 10px',
                      background: 'rgba(255, 255, 255, 0.02)',
                      border: '1px solid var(--border)',
                      borderRadius: '6px',
                      fontSize: '0.8rem',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontWeight: 600 }}>{email.recipientEmail}</span>
                      <Badge variant={email.status === 'SENT' ? 'success' : 'info'}>{email.status}</Badge>
                    </div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                      {email.subject} • {new Date(email.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {/* Quick Shortcuts */}
          <Card>
            <h3 style={{ fontSize: '1rem', margin: '0 0 var(--space-md)' }}>Workspace Shortcuts</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <Button
                variant="secondary"
                onClick={() => navigate('/app/finance')}
                style={{ justifyContent: 'flex-start' }}
              >
                <DollarSign size={16} /> Subscription & Billing
              </Button>
              <Button
                variant="secondary"
                onClick={() => navigate('/app/activity')}
                style={{ justifyContent: 'flex-start' }}
              >
                <FileText size={16} /> Activity Stream
              </Button>
              <Button
                variant="secondary"
                onClick={() => navigate('/app/audit-logs')}
                style={{ justifyContent: 'flex-start' }}
              >
                <Shield size={16} /> Audit Governance
              </Button>
              <Button
                variant="secondary"
                onClick={() => navigate('/app/meetings')}
                style={{ justifyContent: 'flex-start' }}
              >
                <Video size={16} /> Video Conferences
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </>
  )
}
