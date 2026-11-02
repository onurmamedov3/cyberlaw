import {useState} from 'react'
import {NavLink, Outlet, useLocation, useNavigate} from 'react-router-dom'
import {ClipboardCheck, FolderLock, Landmark, LogOut, Menu, MessageSquare, Settings, SquarePen, X} from 'lucide-react'
import {useAuth} from '../lib/auth'
import {useChat} from '../lib/chat'
import {useT} from '../lib/settings'
import type {TKey} from '../lib/i18n'
import {ChatHistoryList} from './ChatHistoryList'
import {ErrorBoundary} from './ErrorBoundary'
import {Logo} from './Logo'
import {LanguageSwitch} from './LanguageSwitch'

const links: { to: string; label: TKey; icon: typeof MessageSquare; admin?: boolean }[] = [
  { to: '/chat', label: 'nav.chat', icon: MessageSquare },
  { to: '/documents', label: 'nav.documents', icon: FolderLock },
  { to: '/compliance', label: 'nav.compliance', icon: ClipboardCheck },
  { to: '/admin', label: 'nav.admin', icon: Landmark, admin: true },
  { to: '/settings', label: 'nav.settings', icon: Settings },
]

export function Layout() {
  const { user, logout } = useAuth()
  const t = useT()
  const location = useLocation()
  const navigate = useNavigate()
  const { newChat, conversationId } = useChat()
  const [open, setOpen] = useState(false)

  const startNewChat = () => {
    newChat()
    navigate('/chat')
    setOpen(false)
  }

  const newChatButton = (
    <button onClick={startNewChat} className="btn btn-neon mb-7 w-full justify-start">
      <SquarePen className="h-4 w-4" /> {t('chat.newChat')}
    </button>
  )

  const nav = (
    <nav className="flex flex-col gap-1" aria-label={t('nav.menu')}>
      {links
        .filter((l) => !l.admin || user?.role === 'ADMIN')
        .map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            // "Chat" goes back to the chat on screen, not to an empty one
            to={to === '/chat' && conversationId ? `/chat/${conversationId}` : to}
            onClick={() => setOpen(false)}
            className={({ isActive }) => `sidebar-nav-link ${isActive ? 'active' : ''}`}
          >
            <Icon className="h-4.5 w-4.5" />
            {t(label)}
          </NavLink>
        ))}
    </nav>
  )

  const footer = (
    <div className="mt-4 border-t border-line pt-4">
      <div className="mb-3 flex items-center gap-3 px-1">
        <div className="grid h-9 w-9 place-items-center rounded-md border border-line bg-panel font-semibold text-neon">
          {user?.displayName.slice(0, 1).toUpperCase()}
        </div>
        <div className="min-w-0">
          <div className="truncate text-sm font-semibold">{user?.displayName}</div>
          <div className="truncate text-xs text-muted">{user?.email}</div>
        </div>
      </div>
      <button onClick={logout} className="btn sidebar-logout w-full">
        <LogOut className="h-4 w-4" /> {t('nav.logout')}
      </button>
    </div>
  )

  return (
    <div className="flex h-full print:block print:h-auto">
      {/* desktop sidebar */}
      <aside className="workspace-sidebar hidden w-64 shrink-0 flex-col px-5 py-7 md:flex print:hidden">
        <div className="mb-9 px-1">
          <Logo />
          <p className="mt-2 text-[11px] text-muted">{t('workspace.research')}</p>
        </div>
        {newChatButton}
        {nav}
        <ChatHistoryList onNavigate={() => setOpen(false)} />
        {footer}
      </aside>

      {/* mobile top bar + drawer */}
      <div className="fixed inset-x-0 top-0 z-30 flex items-center justify-between border-b border-line bg-void px-4 py-3 backdrop-blur md:hidden print:hidden">
        <Logo />
        <div className="flex items-center gap-2">
          <button
            className="btn btn-ghost px-2.5"
            onClick={startNewChat}
            aria-label={t('chat.newChat')}
            title={t('chat.newChat')}
          >
            <SquarePen className="h-5 w-5" />
          </button>
          <button className="btn btn-ghost px-2.5" onClick={() => setOpen(true)} aria-label={t('nav.menu')}>
            <Menu className="h-5 w-5" />
          </button>
        </div>
      </div>
      {open && (
        <div className="fixed inset-0 z-40 bg-black/60 md:hidden" onClick={() => setOpen(false)}>
          <aside className="workspace-sidebar absolute inset-y-0 left-0 flex w-[min(20rem,calc(100vw-2rem))] flex-col p-5" onClick={(e) => e.stopPropagation()}>
            <div className="mb-6 flex items-center justify-between">
              <Logo />
              <button onClick={() => setOpen(false)} aria-label={t('common.close')}>
                <X className="h-5 w-5 text-muted" />
              </button>
            </div>
            {newChatButton}
            {nav}
            <ChatHistoryList onNavigate={() => setOpen(false)} />
            {footer}
          </aside>
        </div>
      )}

      <main className="min-w-0 flex-1 overflow-y-auto pt-16 md:pt-0 print:overflow-visible print:pt-0">
        <header className="workspace-topbar hidden items-center justify-between gap-4 px-8 md:flex print:hidden">
          <div className="flex min-w-0 items-center gap-3 text-xs">
            <span className="text-muted">{t('workspace.label')}</span>
            <span className="text-muted/50" aria-hidden>/</span>
            <span className="truncate font-medium">{t(links.find((link) => location.pathname.startsWith(link.to))?.label ?? 'nav.chat')}</span>
          </div>
          <LanguageSwitch />
        </header>
        {/* a crash shows a message inside the page; going to another page clears it */}
        <ErrorBoundary key={location.pathname}>
          <Outlet />
        </ErrorBoundary>
      </main>
    </div>
  )
}
