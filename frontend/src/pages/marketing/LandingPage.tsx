import { ArrowRight, Building2, CheckCircle2, FileText, Users, Video } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui'

export function LandingPage() {
  const navigate = useNavigate()
  const [isScrolled, setIsScrolled] = useState(false)

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20)
    }

    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const scrollToSection = (id: string) => {
    const element = document.getElementById(id)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }

  return (
    <div className="landing-page">
      {/* Navigation */}
      <nav className={`landing-nav ${isScrolled ? 'landing-nav--scrolled' : ''}`}>
        <div className="container landing-nav__content">
          <div className="landing-nav__brand">
            <Building2 size={24} />
            <span>Digital Office OS</span>
          </div>

          <div className="landing-nav__links">
            <button type="button" onClick={() => scrollToSection('capabilities')}>
              Product
            </button>
            <button type="button" onClick={() => scrollToSection('workflow')}>
              How it works
            </button>
            <button type="button" onClick={() => scrollToSection('faq')}>
              FAQ
            </button>
          </div>

          <div className="landing-nav__actions">
            <Button variant="ghost" onClick={() => navigate('/sign-in')}>
              Sign in
            </Button>
            <Button variant="primary" onClick={() => navigate('/sign-in')}>
              Get started
            </Button>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="landing-hero">
        <div className="container">
          <div className="landing-hero__content">
            <h1 className="landing-hero__title">
              One workspace. Every team. Connected work.
            </h1>
            <p className="landing-hero__subtitle">
              Manage clients, coordinate projects across organizations, keep work moving, review documents, and meet with
              collaborators—all from one connected workspace.
            </p>
            <div className="landing-hero__actions">
              <Button variant="primary" size="lg" onClick={() => navigate('/sign-in')}>
                Open workspace <ArrowRight size={18} />
              </Button>
              <Button variant="secondary" size="lg" onClick={() => scrollToSection('capabilities')}>
                See how it works
              </Button>
            </div>
          </div>

          <div className="landing-hero__visual">
            <div className="hero-preview-card">
              <div className="hero-preview-card__header">
                <span className="hero-preview-dot hero-preview-dot--red" />
                <span className="hero-preview-dot hero-preview-dot--yellow" />
                <span className="hero-preview-dot hero-preview-dot--green" />
              </div>
              <div className="hero-preview-card__content">
                <div className="hero-preview-line hero-preview-line--title" />
                <div className="hero-preview-grid">
                  {[1, 2, 3, 4].map((i) => (
                    <div key={i} className="hero-preview-item">
                      <div className="hero-preview-line hero-preview-line--sm" />
                      <div className="hero-preview-line" />
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Problem & Solution */}
      <section className="landing-section landing-section--alt">
        <div className="container">
          <div className="landing-content">
            <h2 className="landing-section__title">
              Stop losing track of work across disconnected tools
            </h2>
            <p className="landing-section__text">
              When your team uses separate apps for clients, projects, documents, meetings, and approvals, coordination
              becomes a full-time job. Important work falls through the cracks. External partners can't see what they need.
              Status updates live in email threads nobody can find.
            </p>
            <p className="landing-section__text">
              <strong>Digital Office OS brings it all together.</strong> One workspace where internal teams and external
              organizations coordinate on shared projects, track tasks, review documents, and meet—all with clear access
              boundaries and audit trails.
            </p>
          </div>
        </div>
      </section>

      {/* Capabilities */}
      <section id="capabilities" className="landing-section">
        <div className="container">
          <div className="landing-section__header">
            <h2 className="landing-section__title">Everything your team needs to collaborate</h2>
            <p className="landing-section__subtitle">
              From client intake to project delivery, manage the full lifecycle in one place.
            </p>
          </div>

          <div className="capabilities-grid">
            <div className="capability-card">
              <div className="capability-card__icon">
                <Users />
              </div>
              <h3 className="capability-card__title">Organizations & Teams</h3>
              <p className="capability-card__text">
                Manage your organization, invite members, assign roles, and control access at every level.
              </p>
            </div>

            <div className="capability-card">
              <div className="capability-card__icon">
                <Building2 />
              </div>
              <h3 className="capability-card__title">Client Management</h3>
              <p className="capability-card__text">
                Keep client information organized and linked to the projects you're working on together.
              </p>
            </div>

            <div className="capability-card">
              <div className="capability-card__icon">
                <CheckCircle2 />
              </div>
              <h3 className="capability-card__title">Cross-Company Projects</h3>
              <p className="capability-card__text">
                Invite partner organizations to collaborate on specific projects without exposing your private data.
              </p>
            </div>

            <div className="capability-card">
              <div className="capability-card__icon">
                <CheckCircle2 />
              </div>
              <h3 className="capability-card__title">Work & Task Management</h3>
              <p className="capability-card__text">
                Track tasks, workstreams, and milestones. Assign work, leave comments, and see activity in context.
              </p>
            </div>

            <div className="capability-card">
              <div className="capability-card__icon">
                <FileText />
              </div>
              <h3 className="capability-card__title">Documents & Reviews</h3>
              <p className="capability-card__text">
                Upload documents, track versions, assign reviewers, and collect approvals with a clear audit trail.
              </p>
            </div>

            <div className="capability-card">
              <div className="capability-card__icon">
                <Video />
              </div>
              <h3 className="capability-card__title">Meetings & Communication</h3>
              <p className="capability-card__text">
                Create video meetings instantly or schedule them for later. Meet face-to-face without leaving the workspace.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Workflow */}
      <section id="workflow" className="landing-section landing-section--alt">
        <div className="container">
          <div className="landing-section__header">
            <h2 className="landing-section__title">How cross-company collaboration works</h2>
            <p className="landing-section__subtitle">
              Clear boundaries. Secure access. Seamless coordination.
            </p>
          </div>

          <div className="workflow-steps">
            <div className="workflow-step">
              <div className="workflow-step__number">1</div>
              <div className="workflow-step__content">
                <h3>Your organization manages a client</h3>
                <p>Create client records and track the relationship from one place.</p>
              </div>
            </div>

            <div className="workflow-step">
              <div className="workflow-step__number">2</div>
              <div className="workflow-step__content">
                <h3>Create a project for that client</h3>
                <p>Link the project to the client. Add tasks, documents, and meetings specific to that engagement.</p>
              </div>
            </div>

            <div className="workflow-step">
              <div className="workflow-step__number">3</div>
              <div className="workflow-step__content">
                <h3>Invite another organization to collaborate</h3>
                <p>
                  Send a project invitation to a partner organization. They accept and gain access to that project only.
                </p>
              </div>
            </div>

            <div className="workflow-step">
              <div className="workflow-step__number">4</div>
              <div className="workflow-step__content">
                <h3>Coordinate together in the shared workspace</h3>
                <p>
                  Both teams see the same tasks, documents, and activity. Work stays in sync. Unrelated organization data
                  remains private.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section id="faq" className="landing-section">
        <div className="container">
          <div className="landing-section__header">
            <h2 className="landing-section__title">Frequently asked questions</h2>
          </div>

          <div className="faq-list">
            <div className="faq-item">
              <h3 className="faq-question">What is Digital Office OS?</h3>
              <p className="faq-answer">
                Digital Office OS is a unified workspace for managing clients, coordinating projects, organizing work,
                reviewing documents, and meeting with collaborators. It's designed for teams that need to coordinate across
                organizational boundaries while keeping access scoped and secure.
              </p>
            </div>

            <div className="faq-item">
              <h3 className="faq-question">Can multiple organizations work on one project?</h3>
              <p className="faq-answer">
                Yes. When you invite another organization to a project, they receive access to that project's tasks,
                documents, and meetings. They cannot see your other clients, projects, or organization-level data.
              </p>
            </div>

            <div className="faq-item">
              <h3 className="faq-question">Do collaborators get access to all of my organization's data?</h3>
              <p className="faq-answer">
                No. Project-level access is scoped. External collaborators only see the projects they've been invited to.
                Your private clients, internal projects, and organization details remain visible only to your members.
              </p>
            </div>

            <div className="faq-item">
              <h3 className="faq-question">What can organization admins manage?</h3>
              <p className="faq-answer">
                Organization owners and admins can invite members, assign roles, manage subscriptions, view audit logs, and
                control organization-wide settings. Members can participate in assigned projects and tasks according to their
                permissions.
              </p>
            </div>

            <div className="faq-item">
              <h3 className="faq-question">How do I sign in or join an invitation?</h3>
              <p className="faq-answer">
                Digital Office OS uses secure OAuth2 authentication. When you receive an invitation email, click the link to
                accept and create your account. You'll be added to the organization or project immediately after verifying
                your identity.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="landing-section landing-cta">
        <div className="container">
          <div className="landing-cta__content">
            <h2 className="landing-cta__title">Ready to bring your work together?</h2>
            <p className="landing-cta__subtitle">
              Start coordinating with your team and partners in one connected workspace.
            </p>
            <Button variant="primary" size="lg" onClick={() => navigate('/sign-in')}>
              Get started <ArrowRight size={18} />
            </Button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="container">
          <div className="landing-footer__content">
            <div className="landing-footer__brand">
              <Building2 size={20} />
              <span>Digital Office OS</span>
            </div>

            <div className="landing-footer__links">
              <Link to="/sign-in">Sign in</Link>
              <button type="button" onClick={() => scrollToSection('capabilities')}>
                Product
              </button>
              <button type="button" onClick={() => scrollToSection('workflow')}>
                How it works
              </button>
              <button type="button" onClick={() => scrollToSection('faq')}>
                FAQ
              </button>
            </div>
          </div>

          <div className="landing-footer__bottom">
            <p>&copy; {new Date().getFullYear()} Digital Office OS. Built for collaborative teams.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}
