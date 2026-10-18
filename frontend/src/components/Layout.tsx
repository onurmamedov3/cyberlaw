import {useState} from 'react'
import {NavLink, Outlet} from 'react-router-dom'
import {ClipboardCheck, FolderLock, Landmark, LogOut, Menu, MessageSquare, Settings, X} from 'lucide-react'
import {useAuth} from '../lib/auth'
import {useT} from '../lib/settings'
import type {TKey} from '../lib/i18n'
import {Logo} from './Logo'

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
  const [open, setOpen] = useState(false)

  const nav = (
    <nav className="flex flex-1 flex-col gap-1">
      {links
        .filter((l) => !l.admin || user?.role === 'ADMIN')
        .map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={() => setOpen(false)}
            className={({ isActive }) =>
              `group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                isActive
                  ? 'bg-neon/10 text-neon shadow-[inset_2px_0_0_var(--color-neon)]'
                  : 'text-muted hover:bg-white/5 hover:text-white'
              }`
            }
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
        <div className="grid h-9 w-9 place-items-center rounded-full bg-gradient-to-br from-neon to-violet font-bold text-void">
          {user?.displayName.slice(0, 1).toUpperCase()}
        </div>
        <div className="min-w-0">
          <div className="truncate text-sm font-semibold">{user?.displayName}</div>
          <div className="truncate text-xs text-muted">{user?.email}</div>
        </div>
      </div>
      <button onClick={logout} className="btn btn-ghost w-full">
        <LogOut className="h-4 w-4" /> {t('nav.logout')}
      </button>
    </div>
  )

  return (
    <div className="flex h-full">
      {/* desktop sidebar */}
      <aside className="glass m-3 mr-0 hidden w-64 shrink-0 flex-col p-4 md:flex">
        <div className="mb-8 px-1 pt-1">
          <Logo />
        </div>
        {nav}
        {footer}
      </aside>

      {/* mobile top bar + drawer */}
      <div className="fixed inset-x-0 top-0 z-30 flex items-center justify-between border-b border-line bg-void/80 px-4 py-3 backdrop-blur md:hidden">
        <Logo />
        <button className="btn btn-ghost px-2.5" onClick={() => setOpen(true)} aria-label="Menu">
          <Menu className="h-5 w-5" />
        </button>
      </div>
      {open && (
        <div className="fixed inset-0 z-40 bg-black/60 md:hidden" onClick={() => setOpen(false)}>
          <aside className="glass absolute inset-y-3 left-3 flex w-72 flex-col p-4" onClick={(e) => e.stopPropagation()}>
            <div className="mb-8 flex items-center justify-between">
              <Logo />
              <button onClick={() => setOpen(false)} aria-label="Close">
                <X className="h-5 w-5 text-muted" />
              </button>
            </div>
            {nav}
            {footer}
          </aside>
        </div>
      )}

      <main className="min-w-0 flex-1 overflow-y-auto pt-16 md:pt-0">
        <Outlet />
      </main>
    </div>
  )
}
