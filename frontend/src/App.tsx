import {Navigate, Route, Routes} from 'react-router-dom'
import {LoaderCircle} from 'lucide-react'
import {useAuth} from './lib/auth'
import {ChatProvider} from './lib/chat'
import {Layout} from './components/Layout'
import {LoginPage} from './pages/LoginPage'
import {ChatPage} from './pages/ChatPage'
import {DocumentsPage} from './pages/DocumentsPage'
import {CompliancePage} from './pages/CompliancePage'
import {ReportPage} from './pages/ReportPage'
import {AdminPage} from './pages/AdminPage'
import {SettingsPage} from './pages/SettingsPage'

export function App() {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="grid h-full place-items-center">
        <LoaderCircle className="h-10 w-10 animate-spin text-neon" />
      </div>
    )
  }

  if (!user) {
    return (
      <>
        <Routes>
          <Route path="*" element={<LoginPage />} />
        </Routes>
      </>
    )
  }

  return (
    <ChatProvider key={user.id}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/chat/:id" element={<ChatPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/compliance" element={<CompliancePage />} />
          <Route path="/compliance/:id" element={<ReportPage />} />
          {user.role === 'ADMIN' && <Route path="/admin" element={<AdminPage />} />}
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="*" element={<Navigate to="/chat" replace />} />
        </Route>
      </Routes>
    </ChatProvider>
  )
}
