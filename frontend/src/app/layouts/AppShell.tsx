import { Activity, Bell, Building2, CheckCircle2, ChevronLeft, CreditCard, FileText, FolderKanban, LayoutDashboard, LogOut, Menu, Search, Settings, ShieldCheck, Users, Video, X } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../providers/AuthProvider'
import { useWorkspace } from '../providers/WorkspaceProvider'

interface AppShellProps {
  children: React.ReactNode
}

const navGroups = [
  {
    title: 'Workspace',
    items: [
      { label: 'Dashboard', path: '/app', icon: LayoutDashboard },
      { label: 'Organizations', path: '/app/organizations', icon: Building2 },
      { label: 'Projects', path: '/app/projects', icon: FolderKanban },
      { label: 'Clients', path: '/app/clients', icon: Users },
      { label: 'Tasks', path: '/app/tasks', icon: CheckCircle2 },
      { label: 'Finance', path: '/app/finance', icon: CreditCard },
    ],
  },
  {
    title: 'Collaboration',
    items: [
      { label: 'Meetings', path: '/app/meetings', icon: Video },
      { label: 'Documents', path: '/app/documents', icon: FileText },
      { label: 'Activity', path: '/app/activity', icon: Activity },
      { label: 'Audit Logs', path: '/app/audit-logs', icon: ShieldCheck },
    ],
  },
]

export function AppShell({ children }: AppShellProps) {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { user, signOut } = useAuth()
  const { currentOrganization } = useWorkspace()

  const handleSignOut = () => {
    signOut()
    navigate('/sign-in')
  }

  const isActive = (path: string) => {
    if (path === '/app') return location.pathname === '/app'
    return location.pathname.startsWith(path)
  }

  return (
    <div className="app-shell">
      {/* Sidebar - Desktop */}
      <aside className={`app-sidebar ${sidebarOpen ? '' : 'app-sidebar--collapsed'}`}>
        <div className="app-sidebar__header">
          <div className="app-sidebar__brand">
            <Building2 size={24} />
            {sidebarOpen && <span>Digital Office</span>}
          </div>
          <button
            type="button"
            className="btn btn-icon btn-ghost app-sidebar__toggle"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            aria-label={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
          >
            <ChevronLeft size={18} style={{ transform: sidebarOpen ? 'none' : 'rotate(180deg)' }} />
          </button>
        </div>

        {/* Organization Switcher */}
        {sidebarOpen && (
          <div className="app-sidebar__org">
            <button type="button" className="org-switcher" onClick={() => navigate('/app/organizations')}>
              <div className="org-switcher__icon">
                {currentOrganization?.name.charAt(0) || 'W'}
              </div>
              <div className="org-switcher__info">
                <span className="org-switcher__name">{currentOrganization?.name || 'Select workspace'}</span>
                <span className="org-switcher__role">Switch organization</span>
              </div>
            </button>
          </div>
        )}

        {/* Navigation */}
        <nav className="app-sidebar__nav">
          {navGroups.map((group) => (
            <div key={group.title} className="nav-group">
              {sidebarOpen && <div className="nav-group__title">{group.title}</div>}
              {group.items.map((item) => {
                const Icon = item.icon
                const active = isActive(item.path)
                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={`nav-item ${active ? 'nav-item--active' : ''}`}
                    title={!sidebarOpen ? item.label : undefined}
                  >
                    <Icon size={20} />
                    {sidebarOpen && <span>{item.label}</span>}
                  </Link>
                )
              })}
            </div>
          ))}
        </nav>

        {/* Bottom Section */}
        <div className="app-sidebar__bottom">
          <Link
            to="/app/settings"
            className={`nav-item ${isActive('/app/settings') ? 'nav-item--active' : ''}`}
            title={!sidebarOpen ? 'Settings' : undefined}
          >
            <Settings size={20} />
            {sidebarOpen && <span>Settings</span>}
          </Link>
        </div>
      </aside>

      {/* Mobile Menu Overlay */}
      {mobileMenuOpen && (
        <div className="mobile-menu-overlay" onClick={() => setMobileMenuOpen(false)}>
          <div className="mobile-menu" onClick={(e) => e.stopPropagation()}>
            <div className="mobile-menu__header">
              <div className="app-sidebar__brand">
                <Building2 size={24} />
                <span>Digital Office</span>
              </div>
              <button
                type="button"
                className="btn btn-icon btn-ghost"
                onClick={() => setMobileMenuOpen(false)}
                aria-label="Close menu"
              >
                <X size={20} />
              </button>
            </div>

            <nav className="mobile-menu__nav">
              {navGroups.map((group) => (
                <div key={group.title} className="nav-group">
                  <div className="nav-group__title">{group.title}</div>
                  {group.items.map((item) => {
                    const Icon = item.icon
                    const active = isActive(item.path)
                    return (
                      <Link
                        key={item.path}
                        to={item.path}
                        className={`nav-item ${active ? 'nav-item--active' : ''}`}
                        onClick={() => setMobileMenuOpen(false)}
                      >
                        <Icon size={20} />
                        <span>{item.label}</span>
                      </Link>
                    )
                  })}
                </div>
              ))}
            </nav>
          </div>
        </div>
      )}

      {/* Main Content */}
      <div className="app-main">
        {/* Top Bar */}
        <header className="app-topbar">
          <div className="app-topbar__left">
            <button
              type="button"
              className="btn btn-icon btn-ghost mobile-menu-trigger"
              onClick={() => setMobileMenuOpen(true)}
              aria-label="Open menu"
            >
              <Menu size={20} />
            </button>

            <div className="search-trigger">
              <Search size={18} />
              <span>Search...</span>
              <kbd>⌘K</kbd>
            </div>
          </div>

          <div className="app-topbar__right">
            <button
              type="button"
              className="btn btn-icon btn-ghost notification-bell"
              onClick={() => navigate('/app/notifications')}
              aria-label="Notifications"
            >
              <Bell size={20} />
              <span className="notification-badge">3</span>
            </button>

            <div className="user-menu">
              <button type="button" className="user-menu__trigger">
                <div className="user-avatar">
                  {user?.displayName?.charAt(0) || user?.email?.charAt(0) || 'U'}
                </div>
                <span className="user-menu__name">{user?.displayName || user?.email || 'User'}</span>
              </button>

              <div className="user-menu__dropdown">
                <Link to="/app/profile" className="user-menu__item">
                  Profile
                </Link>
                <Link to="/app/settings" className="user-menu__item">
                  Settings
                </Link>
                <hr className="user-menu__divider" />
                <button type="button" className="user-menu__item" onClick={handleSignOut}>
                  <LogOut size={16} />
                  Sign out
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="app-content">{children}</main>
      </div>
    </div>
  )
}
