import {
  ArrowLeft,
  ArrowRight,
  Briefcase,
  Building2,
  Check,
  CheckCircle2,
  CreditCard,
  FileCheck,
  FileText,
  Info,
  Rocket,
  ShieldCheck,
  Upload,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { financeApi, organizationApi } from '../../api'
import { useWorkspace } from '../../app/providers/WorkspaceProvider'
import { Alert, Badge, Button, Card, Input } from '../../components/ui'
import type { OnboardPayload, SubscriptionPlan } from '../../types'

type Step = 1 | 2 | 3 | 4 | 5

interface DocumentUploadState {
  incorporationDoc: File | null
  taxDoc: File | null
  pitchDeckDoc: File | null
}

export function OnboardingWizardPage() {
  const navigate = useNavigate()
  const { setCurrentOrganization } = useWorkspace()

  const [step, setStep] = useState<Step>(1)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [plans, setPlans] = useState<SubscriptionPlan[]>([])

  // Form State
  const [formData, setFormData] = useState<OnboardPayload>({
    name: '',
    slug: '',
    entityType: 'STARTUP',
    legalName: '',
    registrationNumber: '',
    taxId: '',
    jurisdiction: 'United States (Delaware)',
    incorporationDate: '',
    industry: 'Enterprise Software / SaaS',
    companySize: '1-10 (Early Stage)',
    stage: 'Seed / Early Stage',
    website: '',
    linkedinUrl: '',
    country: 'United States',
    city: '',
    address: '',
    postalCode: '',
    signatoryName: '',
    signatoryTitle: 'Founder & CEO',
    signatoryEmail: '',
    signatoryPhone: '',
    incorporationDocName: '',
    taxDocName: '',
    pitchDeckDocName: '',
    planId: '',
    billingInterval: 'MONTHLY',
  })

  const [docs, setDocs] = useState<DocumentUploadState>({
    incorporationDoc: null,
    taxDoc: null,
    pitchDeckDoc: null,
  })

  // Load finance plans
  useEffect(() => {
    const fetchPlans = async () => {
      try {
        const availablePlans = await financeApi.listPlans()
        setPlans(availablePlans)
        if (availablePlans.length > 0) {
          // Default to starter for startup, enterprise for corporate
          const defaultPlan = availablePlans.find((p) =>
            formData.entityType === 'STARTUP' ? p.tier === 'STARTER' || p.tier === 'FREE' : p.tier === 'ENTERPRISE' || p.tier === 'PROFESSIONAL'
          ) || availablePlans[0]
          setFormData((prev) => ({ ...prev, planId: defaultPlan.id }))
        }
      } catch (err) {
        console.warn('Could not load subscription plans from finance service', err)
      }
    }
    void fetchPlans()
  }, [formData.entityType])

  const handleNameChange = (name: string) => {
    const slug = name
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9-]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 80)
    setFormData((prev) => ({
      ...prev,
      name,
      legalName: prev.legalName || name,
      slug,
    }))
  }

  const handleFileUpload = (
    field: 'incorporationDoc' | 'taxDoc' | 'pitchDeckDoc',
    nameField: 'incorporationDocName' | 'taxDocName' | 'pitchDeckDocName',
    file: File | null
  ) => {
    if (file) {
      if (file.size > 25 * 1024 * 1024) {
        setError(`File "${file.name}" exceeds the 25MB limit.`)
        return
      }
      setDocs((prev) => ({ ...prev, [field]: file }))
      setFormData((prev) => ({ ...prev, [nameField]: file.name }))
    } else {
      setDocs((prev) => ({ ...prev, [field]: null }))
      setFormData((prev) => ({ ...prev, [nameField]: '' }))
    }
  }

  const validateStep = (currentStep: Step): boolean => {
    setError(null)
    if (currentStep === 1) {
      if (!formData.name.trim()) {
        setError('Organization Name is required.')
        return false
      }
      if (!formData.industry) {
        setError('Industry classification is required.')
        return false
      }
    }
    if (currentStep === 2) {
      if (!formData.legalName?.trim()) {
        setError('Legal Registered Name is required.')
        return false
      }
      if (!formData.registrationNumber?.trim()) {
        setError('Business Registration Number (CIN / EIN / Reg #) is required for corporate onboarding.')
        return false
      }
      if (!formData.country?.trim()) {
        setError('Country of Registration is required.')
        return false
      }
      if (!formData.signatoryName?.trim()) {
        setError('Authorized Signatory Full Name is required.')
        return false
      }
      if (!formData.signatoryEmail?.trim()) {
        setError('Authorized Signatory Email is required.')
        return false
      }
    }
    if (currentStep === 3) {
      if (!formData.incorporationDocName) {
        setError('Please upload your Certificate of Incorporation or Business Registration document.')
        return false
      }
    }
    return true
  }

  const handleNext = () => {
    if (validateStep(step)) {
      setStep((prev) => Math.min(5, prev + 1) as Step)
    }
  }

  const handleBack = () => {
    setError(null)
    setStep((prev) => Math.max(1, prev - 1) as Step)
  }

  const handleSubmit = async () => {
    if (!validateStep(step)) return
    setLoading(true)
    setError(null)

    try {
      // 1. Submit Onboard Data to organization-service
      const res = await organizationApi.onboard(formData)

      // 2. If a plan was selected, link it via finance-service
      if (formData.planId && res.organization.id) {
        try {
          await financeApi.createSubscription(
            res.organization.id,
            formData.planId,
            formData.billingInterval || 'MONTHLY'
          )
        } catch (planErr) {
          console.warn('Subscription assignment deferred:', planErr)
        }
      }

      // 3. Set newly created org in WorkspaceProvider
      setCurrentOrganization({
        id: res.organization.id,
        name: res.organization.name,
        membership: null,
      })

      // 4. Navigate directly to the Organization Detail page or Dashboard
      navigate(`/app/organizations/${res.organization.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Onboarding submission failed. Please review your entries.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="onboarding-page" style={{ maxWidth: '960px', margin: '0 auto', padding: '24px 16px' }}>
      {/* Header */}
      <div style={{ marginBottom: '32px', textAlign: 'center' }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', color: 'var(--color-primary)', marginBottom: '8px' }}>
          <ShieldCheck size={28} />
          <span style={{ fontWeight: 700, letterSpacing: '0.05em', textTransform: 'uppercase', fontSize: '0.85rem' }}>
            Enterprise & Startup Compliance
          </span>
        </div>
        <h1 style={{ fontSize: '2rem', fontWeight: 800, margin: '4px 0 8px', color: 'var(--text-primary)' }}>
          Organization Onboarding & KYC
        </h1>
        <p style={{ color: 'var(--text-muted)', maxWidth: '640px', margin: '0 auto', fontSize: '0.95rem' }}>
          Complete entity verification, register legal credentials, upload compliance documents, and configure your multi-tenant workspace.
        </p>
      </div>

      {/* Stepper Indicator */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '36px',
          position: 'relative',
        }}
      >
        <div
          style={{
            position: 'absolute',
            top: '20px',
            left: '40px',
            right: '40px',
            height: '2px',
            background: 'var(--border)',
            zIndex: 0,
          }}
        />
        {[
          { num: 1, title: 'Classification', icon: Building2 },
          { num: 2, title: 'Registration', icon: Briefcase },
          { num: 3, title: 'Documents', icon: FileCheck },
          { num: 4, title: 'Plan & Tier', icon: CreditCard },
          { num: 5, title: 'Confirmation', icon: CheckCircle2 },
        ].map((s) => {
          const isDone = step > s.num
          const isCurrent = step === s.num
          const Icon = s.icon
          return (
            <div
              key={s.num}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                position: 'relative',
                zIndex: 1,
              }}
            >
              <div
                style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: '50%',
                  background: isCurrent
                    ? 'var(--color-primary)'
                    : isDone
                    ? 'rgba(16, 185, 129, 0.9)'
                    : 'var(--bg-secondary)',
                  border: isCurrent
                    ? '3px solid rgba(99, 102, 241, 0.3)'
                    : '2px solid var(--border)',
                  color: isCurrent || isDone ? '#fff' : 'var(--text-muted)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 700,
                  transition: 'all 0.2s',
                }}
              >
                {isDone ? <Check size={20} /> : <Icon size={18} />}
              </div>
              <span
                style={{
                  fontSize: '0.8rem',
                  fontWeight: isCurrent ? 700 : 500,
                  marginTop: '8px',
                  color: isCurrent ? 'var(--text-primary)' : 'var(--text-muted)',
                }}
              >
                {s.title}
              </span>
            </div>
          )
        })}
      </div>

      {error && (
        <div style={{ marginBottom: '24px' }}>
          <Alert variant="error">{error}</Alert>
        </div>
      )}

      {/* STEP 1: Entity Classification & Basic Details */}
      {step === 1 && (
        <Card style={{ padding: '32px' }}>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: '6px' }}>
            1. Organization Classification
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', fontSize: '0.9rem' }}>
            Select whether you are onboarding as an established Corporate enterprise or a high-growth Startup venture.
          </p>

          {/* Type Selectors */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '28px' }}>
            <div
              onClick={() =>
                setFormData((prev) => ({
                  ...prev,
                  entityType: 'STARTUP',
                  companySize: '1-10 (Early Stage)',
                  stage: 'Seed / Early Stage',
                }))
              }
              style={{
                border: formData.entityType === 'STARTUP' ? '2px solid var(--color-primary)' : '1px solid var(--border)',
                background: formData.entityType === 'STARTUP' ? 'rgba(99, 102, 241, 0.05)' : 'var(--bg-secondary)',
                borderRadius: '12px',
                padding: '24px',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                <div
                  style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '10px',
                    background: 'rgba(99, 102, 241, 0.15)',
                    color: 'var(--color-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <Rocket size={24} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700 }}>Startup / Venture</h3>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Founders, Pre-Seed to Series B</span>
                </div>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: 0, lineHeight: 1.4 }}>
                Optimized for agile collaboration, rapid investor/client sharing, pitch deck reviews, and lean workspace management.
              </p>
            </div>

            <div
              onClick={() =>
                setFormData((prev) => ({
                  ...prev,
                  entityType: 'CORPORATE',
                  companySize: '200-500',
                  stage: 'Enterprise / Corporate',
                }))
              }
              style={{
                border: formData.entityType === 'CORPORATE' ? '2px solid var(--color-primary)' : '1px solid var(--border)',
                background: formData.entityType === 'CORPORATE' ? 'rgba(99, 102, 241, 0.05)' : 'var(--bg-secondary)',
                borderRadius: '12px',
                padding: '24px',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                <div
                  style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '10px',
                    background: 'rgba(16, 185, 129, 0.15)',
                    color: '#10b981',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <Building2 size={24} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700 }}>Corporate Enterprise</h3>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Established Businesses & Groups</span>
                </div>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: 0, lineHeight: 1.4 }}>
                Full compliance governance, strict document review pipelines, corporate tax verification, and unlimited team hierarchy.
              </p>
            </div>
          </div>

          {/* Core Information */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '18px', marginBottom: '20px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Organization Brand / Display Name *
              </label>
              <Input
                placeholder="e.g. Acme Technologies"
                value={formData.name}
                onChange={(e) => handleNameChange(e.target.value)}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Workspace Slug (Unique URI Identifier) *
              </label>
              <Input
                placeholder="acme-technologies"
                value={formData.slug}
                onChange={(e) => setFormData((prev) => ({ ...prev, slug: e.target.value }))}
              />
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginTop: '4px' }}>
                digitaloffice.com/{formData.slug || 'workspace-slug'}
              </span>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '18px', marginBottom: '20px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Industry / Sector *
              </label>
              <select
                className="input"
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}
                value={formData.industry || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, industry: e.target.value }))}
              >
                <option value="Enterprise Software / SaaS">Enterprise Software / SaaS</option>
                <option value="Artificial Intelligence / ML">Artificial Intelligence / ML</option>
                <option value="Financial Services & Fintech">Financial Services & Fintech</option>
                <option value="Healthcare & Life Sciences">Healthcare & Life Sciences</option>
                <option value="CleanTech & Renewable Energy">CleanTech & Renewable Energy</option>
                <option value="E-Commerce & Retail Tech">E-Commerce & Retail Tech</option>
                <option value="Supply Chain & Logistics">Supply Chain & Logistics</option>
                <option value="Consulting & Professional Services">Consulting & Professional Services</option>
                <option value="Media & Entertainment">Media & Entertainment</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Company Size / Headcount
              </label>
              <select
                className="input"
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}
                value={formData.companySize || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, companySize: e.target.value }))}
              >
                <option value="1-10 (Early Stage)">1-10 (Early Stage)</option>
                <option value="11-50 (Growth)">11-50 (Growth)</option>
                <option value="51-200 (Scale-up)">51-200 (Scale-up)</option>
                <option value="201-500 (Mid-Enterprise)">201-500 (Mid-Enterprise)</option>
                <option value="501-1000">501-1000</option>
                <option value="1000+ (Multinational)">1000+ (Multinational)</option>
              </select>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '18px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Website URL
              </label>
              <Input
                placeholder="https://example.com"
                value={formData.website || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, website: e.target.value }))}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                LinkedIn / Crunchbase Profile
              </label>
              <Input
                placeholder="https://linkedin.com/company/acme"
                value={formData.linkedinUrl || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, linkedinUrl: e.target.value }))}
              />
            </div>
          </div>
        </Card>
      )}

      {/* STEP 2: Legal Registration & Entity Details */}
      {step === 2 && (
        <Card style={{ padding: '32px' }}>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: '6px' }}>
            2. Legal Registration & Compliance Details
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', fontSize: '0.9rem' }}>
            Provide registered company credentials, statutory identifiers, and authorized signatory information.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '18px', marginBottom: '20px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Legal Registered Entity Name *
              </label>
              <Input
                placeholder="e.g. Acme Technologies Inc. / Pvt Ltd"
                value={formData.legalName || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, legalName: e.target.value }))}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Business Registration Number (CIN / EIN / LLPIN / Reg #) *
              </label>
              <Input
                placeholder="e.g. U72200MH2021PTC123456 or 12-3456789"
                value={formData.registrationNumber || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, registrationNumber: e.target.value }))}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '18px', marginBottom: '20px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Tax Identification Number (GSTIN / VAT / TIN / EIN) *
              </label>
              <Input
                placeholder="e.g. 27AABCU9603R1ZN or US-EIN-987654"
                value={formData.taxId || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, taxId: e.target.value }))}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Jurisdiction of Incorporation
              </label>
              <Input
                placeholder="e.g. Delaware (US), England & Wales (UK), India"
                value={formData.jurisdiction || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, jurisdiction: e.target.value }))}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '18px', marginBottom: '20px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Date of Incorporation
              </label>
              <Input
                type="date"
                value={formData.incorporationDate || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, incorporationDate: e.target.value }))}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Country of Operation / Headquarters *
              </label>
              <Input
                placeholder="e.g. United States, United Kingdom, India, Germany"
                value={formData.country || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, country: e.target.value }))}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '18px', marginBottom: '28px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Registered Office Address
              </label>
              <Input
                placeholder="Street address, Suite, Building"
                value={formData.address || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, address: e.target.value }))}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                City & Postal Code
              </label>
              <div style={{ display: 'flex', gap: '8px' }}>
                <Input
                  placeholder="City"
                  value={formData.city || ''}
                  onChange={(e) => setFormData((prev) => ({ ...prev, city: e.target.value }))}
                />
                <Input
                  placeholder="ZIP / PIN"
                  style={{ width: '110px' }}
                  value={formData.postalCode || ''}
                  onChange={(e) => setFormData((prev) => ({ ...prev, postalCode: e.target.value }))}
                />
              </div>
            </div>
          </div>

          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '14px', borderTop: '1px solid var(--border)', paddingTop: '20px' }}>
            Authorized Officer / Representative Details
          </h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '18px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Representative Full Name *
              </label>
              <Input
                placeholder="e.g. Johnathan Doe"
                value={formData.signatoryName || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, signatoryName: e.target.value }))}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Designation / Title
              </label>
              <Input
                placeholder="e.g. Chief Executive Officer / Managing Director"
                value={formData.signatoryTitle || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, signatoryTitle: e.target.value }))}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Official Corporate Email *
              </label>
              <Input
                type="email"
                placeholder="official@company.com"
                value={formData.signatoryEmail || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, signatoryEmail: e.target.value }))}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
                Direct Phone Number
              </label>
              <Input
                placeholder="+1 (555) 000-0000"
                value={formData.signatoryPhone || ''}
                onChange={(e) => setFormData((prev) => ({ ...prev, signatoryPhone: e.target.value }))}
              />
            </div>
          </div>
        </Card>
      )}

      {/* STEP 3: Document Uploads & KYC Verification */}
      {step === 3 && (
        <Card style={{ padding: '32px' }}>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: '6px' }}>
            3. Verification Documents (KYC & Legal Proof)
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', fontSize: '0.9rem' }}>
            Upload legal credentials to verify your business identity and enable cross-company project capabilities. Supported formats: PDF, PNG, JPG (Max 25MB).
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Document 1: Certificate of Incorporation */}
            <div
              style={{
                border: docs.incorporationDoc ? '1px solid #10b981' : '1px dashed var(--border)',
                background: docs.incorporationDoc ? 'rgba(16, 185, 129, 0.04)' : 'var(--bg-secondary)',
                borderRadius: '10px',
                padding: '20px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div
                  style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '8px',
                    background: docs.incorporationDoc ? 'rgba(16, 185, 129, 0.15)' : 'rgba(99, 102, 241, 0.1)',
                    color: docs.incorporationDoc ? '#10b981' : 'var(--color-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <FileText size={22} />
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <h4 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700 }}>
                      Certificate of Incorporation / Registration Proof *
                    </h4>
                    <Badge variant="warning">Mandatory</Badge>
                  </div>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>
                    {docs.incorporationDoc
                      ? `Attached: ${docs.incorporationDoc.name} (${(docs.incorporationDoc.size / 1024).toFixed(1)} KB)`
                      : 'Government issued certificate proving incorporation or business establishment.'}
                  </span>
                </div>
              </div>

              <div>
                <label className="btn btn-secondary" style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                  <Upload size={16} />
                  <span>{docs.incorporationDoc ? 'Replace' : 'Upload File'}</span>
                  <input
                    type="file"
                    accept=".pdf,.png,.jpg,.jpeg"
                    style={{ display: 'none' }}
                    onChange={(e) => handleFileUpload('incorporationDoc', 'incorporationDocName', e.target.files?.[0] || null)}
                  />
                </label>
              </div>
            </div>

            {/* Document 2: Tax Registration / Exemption */}
            <div
              style={{
                border: docs.taxDoc ? '1px solid #10b981' : '1px dashed var(--border)',
                background: docs.taxDoc ? 'rgba(16, 185, 129, 0.04)' : 'var(--bg-secondary)',
                borderRadius: '10px',
                padding: '20px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div
                  style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '8px',
                    background: docs.taxDoc ? 'rgba(16, 185, 129, 0.15)' : 'rgba(99, 102, 241, 0.1)',
                    color: docs.taxDoc ? '#10b981' : 'var(--color-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <FileCheck size={22} />
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <h4 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700 }}>
                      Tax Registration Certificate (GST / VAT / EIN Document)
                    </h4>
                    <Badge variant="info">Recommended</Badge>
                  </div>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>
                    {docs.taxDoc
                      ? `Attached: ${docs.taxDoc.name} (${(docs.taxDoc.size / 1024).toFixed(1)} KB)`
                      : 'Statutory tax registration document or letter of tax exemption.'}
                  </span>
                </div>
              </div>

              <div>
                <label className="btn btn-secondary" style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                  <Upload size={16} />
                  <span>{docs.taxDoc ? 'Replace' : 'Upload File'}</span>
                  <input
                    type="file"
                    accept=".pdf,.png,.jpg,.jpeg"
                    style={{ display: 'none' }}
                    onChange={(e) => handleFileUpload('taxDoc', 'taxDocName', e.target.files?.[0] || null)}
                  />
                </label>
              </div>
            </div>

            {/* Document 3: Pitch Deck / Corporate Capability Deck */}
            <div
              style={{
                border: docs.pitchDeckDoc ? '1px solid #10b981' : '1px dashed var(--border)',
                background: docs.pitchDeckDoc ? 'rgba(16, 185, 129, 0.04)' : 'var(--bg-secondary)',
                borderRadius: '10px',
                padding: '20px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div
                  style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '8px',
                    background: docs.pitchDeckDoc ? 'rgba(16, 185, 129, 0.15)' : 'rgba(99, 102, 241, 0.1)',
                    color: docs.pitchDeckDoc ? '#10b981' : 'var(--color-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <Briefcase size={22} />
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <h4 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700 }}>
                      {formData.entityType === 'STARTUP' ? 'Pitch Deck / Executive Summary' : 'Corporate Capability Deck'}
                    </h4>
                    <Badge variant="info">Optional</Badge>
                  </div>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>
                    {docs.pitchDeckDoc
                      ? `Attached: ${docs.pitchDeckDoc.name} (${(docs.pitchDeckDoc.size / 1024).toFixed(1)} KB)`
                      : 'Overview presentation shared with partners, clients, or investors during cross-company collaboration.'}
                  </span>
                </div>
              </div>

              <div>
                <label className="btn btn-secondary" style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                  <Upload size={16} />
                  <span>{docs.pitchDeckDoc ? 'Replace' : 'Upload File'}</span>
                  <input
                    type="file"
                    accept=".pdf,.png,.jpg,.jpeg,.pptx"
                    style={{ display: 'none' }}
                    onChange={(e) => handleFileUpload('pitchDeckDoc', 'pitchDeckDocName', e.target.files?.[0] || null)}
                  />
                </label>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* STEP 4: Subscription Plan & Workspace Tier */}
      {step === 4 && (
        <Card style={{ padding: '32px' }}>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: '6px' }}>
            4. Choose Your Workspace Tier
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', fontSize: '0.9rem' }}>
            Select the plan that matches your organization's team size, project scale, and compliance requirements.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '20px' }}>
            {plans.map((p) => {
              const isSelected = formData.planId === p.id
              const isRecommended = formData.entityType === 'STARTUP' ? p.tier === 'STARTER' : p.tier === 'ENTERPRISE'
              return (
                <div
                  key={p.id}
                  onClick={() => setFormData((prev) => ({ ...prev, planId: p.id }))}
                  style={{
                    border: isSelected ? '2px solid var(--color-primary)' : '1px solid var(--border)',
                    background: isSelected ? 'rgba(99, 102, 241, 0.05)' : 'var(--bg-secondary)',
                    borderRadius: '12px',
                    padding: '24px',
                    cursor: 'pointer',
                    position: 'relative',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                  }}
                >
                  {isRecommended && (
                    <span
                      style={{
                        position: 'absolute',
                        top: '-10px',
                        right: '16px',
                        background: 'var(--color-primary)',
                        color: '#fff',
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        padding: '2px 8px',
                        borderRadius: '12px',
                        textTransform: 'uppercase',
                      }}
                    >
                      Recommended for {formData.entityType === 'STARTUP' ? 'Startups' : 'Corporates'}
                    </span>
                  )}
                  <div>
                    <h3 style={{ margin: '0 0 4px', fontSize: '1.2rem', fontWeight: 800 }}>{p.displayName}</h3>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', minHeight: '38px', margin: 0 }}>
                      {p.description}
                    </p>
                    <div style={{ margin: '20px 0', fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                      ${p.price}
                      <span style={{ fontSize: '0.9rem', fontWeight: 500, color: 'var(--text-muted)' }}>
                        /{p.billingInterval.toLowerCase()}
                      </span>
                    </div>

                    <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 20px', fontSize: '0.85rem', lineHeight: 1.8 }}>
                      <li style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Check size={16} color="#10b981" /> Up to {p.maxMembers} organization members
                      </li>
                      <li style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Check size={16} color="#10b981" /> {p.maxProjects} active shared projects
                      </li>
                      <li style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Check size={16} color="#10b981" /> {(p.storageLimitMb / 1024).toFixed(0)} GB encrypted storage
                      </li>
                      <li style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Check size={16} color="#10b981" /> Up to {p.maxMeetingParticipants} video participants
                      </li>
                    </ul>
                  </div>

                  <Button
                    variant={isSelected ? 'primary' : 'secondary'}
                    style={{ width: '100%' }}
                    onClick={() => setFormData((prev) => ({ ...prev, planId: p.id }))}
                  >
                    {isSelected ? 'Selected Tier' : 'Choose Plan'}
                  </Button>
                </div>
              )
            })}
          </div>
        </Card>
      )}

      {/* STEP 5: Review & Confirmation */}
      {step === 5 && (
        <Card style={{ padding: '32px' }}>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: '6px' }}>
            5. Review & Complete Onboarding
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', fontSize: '0.9rem' }}>
            Please confirm your entity details and compliance documents before activating your workspace.
          </p>

          <div
            style={{
              background: 'var(--bg-secondary)',
              borderRadius: '10px',
              padding: '24px',
              marginBottom: '24px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 800 }}>{formData.name}</h3>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Slug: {formData.slug} | Type: {formData.entityType}
                </span>
              </div>
              <Badge variant={formData.entityType === 'CORPORATE' ? 'info' : 'success'}>
                {formData.entityType}
              </Badge>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', fontSize: '0.9rem' }}>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Legal Registered Name</span>
                <strong>{formData.legalName || formData.name}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Registration Number (CIN/EIN)</span>
                <strong>{formData.registrationNumber || 'Not provided'}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Tax Identification (GSTIN/VAT)</span>
                <strong>{formData.taxId || 'Not provided'}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Jurisdiction / Country</span>
                <strong>{formData.jurisdiction}, {formData.country}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Authorized Representative</span>
                <strong>{formData.signatoryName} ({formData.signatoryTitle})</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Signatory Email</span>
                <strong>{formData.signatoryEmail}</strong>
              </div>
            </div>

            <div style={{ marginTop: '20px', borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem', marginBottom: '8px' }}>
                Uploaded Compliance Documents
              </span>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px' }}>
                <Badge variant="success">
                  <Check size={14} style={{ marginRight: '4px' }} /> {formData.incorporationDocName || 'Certificate of Incorporation'}
                </Badge>
                {formData.taxDocName && (
                  <Badge variant="info">
                    <Check size={14} style={{ marginRight: '4px' }} /> {formData.taxDocName}
                  </Badge>
                )}
                {formData.pitchDeckDocName && (
                  <Badge variant="info">
                    <Check size={14} style={{ marginRight: '4px' }} /> {formData.pitchDeckDocName}
                  </Badge>
                )}
              </div>
            </div>
          </div>

          <div
            style={{
              padding: '16px',
              border: '1px solid rgba(99, 102, 241, 0.2)',
              borderRadius: '8px',
              background: 'rgba(99, 102, 241, 0.04)',
              marginBottom: '24px',
              fontSize: '0.85rem',
              color: 'var(--text-secondary)',
            }}
          >
            <div style={{ display: 'flex', gap: '10px' }}>
              <Info size={18} color="var(--color-primary)" style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>
                <strong>Compliance & Multi-Tenant Isolation Guarantee:</strong>
                <p style={{ margin: '4px 0 0', lineHeight: 1.4 }}>
                  By completing onboarding, your organization receives its dedicated multi-tenant security boundary. Projects, documents, and client communications will remain strictly isolated and protected under your legal governance policies.
                </p>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Navigation Buttons */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '28px' }}>
        {step > 1 ? (
          <Button variant="secondary" onClick={handleBack} disabled={loading}>
            <ArrowLeft size={16} /> Back
          </Button>
        ) : (
          <Button variant="ghost" onClick={() => navigate('/app/organizations')}>
            Cancel
          </Button>
        )}

        {step < 5 ? (
          <Button variant="primary" onClick={handleNext}>
            Continue <ArrowRight size={16} />
          </Button>
        ) : (
          <Button variant="primary" onClick={() => void handleSubmit()} loading={loading}>
            <ShieldCheck size={18} /> Complete Onboarding & Launch Workspace
          </Button>
        )}
      </div>
    </div>
  )
}
