import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router-dom'
import { AuthProvider } from './app/providers/AuthProvider'
import { WorkspaceProvider } from './app/providers/WorkspaceProvider'
import { ProtectedRoute } from './app/guards/ProtectedRoute'
import { PublicRoute } from './app/guards/PublicRoute'
import { AppShell } from './app/layouts/AppShell'
import { LandingPage } from './pages/marketing/LandingPage'
import { SignInPage } from './pages/auth/SignInPage'
import { RegisterPage } from './pages/auth/RegisterPage'
import { Dashboard } from './pages/app/Dashboard'
import { OrganizationsPage } from './pages/organizations/OrganizationsPage'
import { CreateOrganizationPage } from './pages/organizations/CreateOrganizationPage'
import { OrganizationDetailPage } from './pages/organizations/OrganizationDetailPage'
import { OnboardingWizardPage } from './pages/organizations/OnboardingWizardPage'
import { ClientsPage } from './pages/clients/ClientsPage'
import { CreateClientPage } from './pages/clients/CreateClientPage'
import { ProjectsPage } from './pages/projects/ProjectsPage'
import { CreateProjectPage } from './pages/projects/CreateProjectPage'
import { ProjectDetailPage } from './pages/projects/ProjectDetailPage'
import { TasksPage } from './pages/work/TasksPage'
import { DocumentsPage } from './pages/documents/DocumentsPage'
import { MeetingDashboard } from './pages/MeetingDashboard'
import { MeetingRoom } from './pages/MeetingRoom'
import { FinanceDashboard } from './pages/FinanceDashboard'
import { NotificationCenter } from './pages/NotificationCenter'
import { NotificationPreferences } from './pages/NotificationPreferences'
import { ActivityFeedPage } from './pages/ActivityFeedPage'
import { AuditLogViewer } from './pages/AuditLogViewer'
import './App.css'

function MeetingLegacyRedirect() {
  const { meetingId } = useParams()
  return <Navigate to={`/app/meetings/${meetingId}`} replace />
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <WorkspaceProvider>
          <Routes>
            {/* Public routes */}
            <Route
              path="/"
              element={
                <PublicRoute>
                  <LandingPage />
                </PublicRoute>
              }
            />
            <Route
              path="/sign-in"
              element={
                <PublicRoute>
                  <SignInPage />
                </PublicRoute>
              }
            />
            <Route
              path="/register"
              element={
                <PublicRoute>
                  <RegisterPage />
                </PublicRoute>
              }
            />

            {/* Protected routes with app shell */}
            <Route
              path="/app"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <Dashboard />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/organizations"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <OrganizationsPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/organizations/new"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <CreateOrganizationPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/onboarding"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <OnboardingWizardPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/organizations/:id"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <OrganizationDetailPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/clients"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <ClientsPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/clients/new"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <CreateClientPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/projects"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <ProjectsPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/projects/new"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <CreateProjectPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/projects/:id"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <ProjectDetailPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/tasks"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <TasksPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/documents"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <DocumentsPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/meetings"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <MeetingDashboard />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/meetings/:meetingId"
              element={
                <ProtectedRoute>
                  <MeetingRoom />
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/finance"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <FinanceDashboard />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/notifications"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <NotificationCenter />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/settings/notifications"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <NotificationPreferences />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/activity"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <ActivityFeedPage />
                  </AppShell>
                </ProtectedRoute>
              }
            />
            <Route
              path="/app/audit-logs"
              element={
                <ProtectedRoute>
                  <AppShell>
                    <AuditLogViewer />
                  </AppShell>
                </ProtectedRoute>
              }
            />

            {/* Backward compat redirects for old routes */}
            <Route path="/onboarding" element={<Navigate to="/app/onboarding" replace />} />
            <Route path="/meetings" element={<Navigate to="/app/meetings" replace />} />
            <Route path="/meetings/:meetingId" element={<MeetingLegacyRedirect />} />
            <Route path="/finance" element={<Navigate to="/app/finance" replace />} />
            <Route path="/notifications" element={<Navigate to="/app/notifications" replace />} />
            <Route path="/settings" element={<Navigate to="/app/settings/notifications" replace />} />
            <Route path="/settings/notifications" element={<Navigate to="/app/settings/notifications" replace />} />
            <Route path="/activity" element={<Navigate to="/app/activity" replace />} />
            <Route path="/audit-logs" element={<Navigate to="/app/audit-logs" replace />} />

            {/* Common shortcuts & redirects */}
            <Route path="/app/settings" element={<Navigate to="/app/settings/notifications" replace />} />
            <Route path="/app/profile" element={<Navigate to="/app" replace />} />
            <Route path="/app/documents/upload" element={<Navigate to="/app/documents" replace />} />
            <Route path="/app/tasks/new" element={<Navigate to="/app/tasks" replace />} />

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/app" replace />} />
          </Routes>
        </WorkspaceProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
