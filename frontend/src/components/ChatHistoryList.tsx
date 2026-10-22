import {type KeyboardEvent, useState} from 'react'
import {Link, useLocation, useNavigate} from 'react-router-dom'
import {Check, Ellipsis, Pencil, Trash, X} from 'lucide-react'
import {useChat} from '../lib/chat'
import {useT} from '../lib/settings'
import type {TKey} from '../lib/i18n'
import type {Conversation} from '../lib/types'
import {useConfirm} from './ConfirmDialog'
import {ErrorNote} from './ErrorNote'

const DAY = 86_400_000

/** Today / Yesterday / Previous 7 days / Older, by the time of the last answer. */
function groups(conversations: Conversation[]) {
  const startOfToday = new Date().setHours(0, 0, 0, 0)
  const groupOf = (c: Conversation): TKey => {
    const time = new Date(c.updatedAt).getTime()
    if (time >= startOfToday) return 'history.today'
    if (time >= startOfToday - DAY) return 'history.yesterday'
    if (time >= startOfToday - 7 * DAY) return 'history.week'
    return 'history.older'
  }
  const result: { label: TKey; items: Conversation[] }[] = []
  for (const c of conversations) {
    const label = groupOf(c)
    const last = result[result.length - 1]
    if (last?.label === label) last.items.push(c)
    else result.push({ label, items: [c] })
  }
  return result
}

/** The saved chats in the sidebar; each can be renamed or deleted. */
export function ChatHistoryList({ onNavigate }: { onNavigate: () => void }) {
  const t = useT()
  const { conversations, conversationId, renameConversation, deleteConversation } = useChat()
  const confirm = useConfirm()
  const navigate = useNavigate()
  const location = useLocation()
  const [actionsFor, setActionsFor] = useState<string | null>(null)
  const [editing, setEditing] = useState<{ id: string; title: string } | null>(null)
  const [error, setError] = useState<unknown>(null)

  const saveTitle = async () => {
    if (!editing) return
    const { id, title } = editing
    setEditing(null)
    const current = conversations?.find((c) => c.id === id)?.title
    if (!title.trim() || title.trim() === current) return
    try {
      setError(null)
      await renameConversation(id, title.trim())
    } catch (e) {
      setError(e)
    }
  }

  const onKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') saveTitle()
    if (e.key === 'Escape') setEditing(null)
  }

  const remove = async (c: Conversation) => {
    setActionsFor(null)
    if (!(await confirm({ message: t('history.confirmDelete'), confirmLabel: t('history.delete') }))) return
    try {
      setError(null)
      await deleteConversation(c.id)
      if (location.pathname === `/chat/${c.id}`) navigate('/chat')
    } catch (e) {
      setError(e)
    }
  }

  return (
    <div className="mt-5 flex min-h-0 flex-1 flex-col">
      <div className="label-mono mb-2 px-3">{t('history.recent')}</div>
      <ErrorNote error={error} className="mb-2 text-xs" />
      <div className="-mr-2 min-h-0 flex-1 overflow-y-auto pr-2">
        {conversations?.length === 0 && <p className="px-3 text-xs text-muted">{t('history.empty')}</p>}
        {conversations &&
          groups(conversations).map(({ label, items }) => (
            <div key={label} className="mb-3">
              <div className="px-3 pb-1 text-[11px] font-semibold uppercase tracking-wide text-muted/70">{t(label)}</div>
              {items.map((c) => {
                const active = c.id === conversationId && location.pathname.startsWith('/chat')
                const showActions = actionsFor === c.id
                return (
                  <div
                    key={c.id}
                    className={`group flex items-center gap-1 rounded-lg transition-colors ${
                      active ? 'bg-neon/10 text-neon' : 'text-slate-300 hover:bg-white/5 hover:text-white'
                    }`}
                  >
                    {editing?.id === c.id ? (
                      <input
                        autoFocus
                        value={editing.title}
                        maxLength={200}
                        onChange={(e) => setEditing({ id: c.id, title: e.target.value })}
                        onKeyDown={onKey}
                        onBlur={saveTitle}
                        aria-label={t('history.rename')}
                        className="input min-w-0 flex-1 px-2 py-1.5 text-sm"
                      />
                    ) : (
                      <Link
                        to={`/chat/${c.id}`}
                        onClick={onNavigate}
                        title={c.title}
                        className="min-w-0 flex-1 truncate px-3 py-2 text-sm"
                      >
                        {c.title}
                      </Link>
                    )}
                    {showActions ? (
                      <>
                        <button
                          className="rounded-md p-1.5 text-muted hover:text-white"
                          onClick={() => {
                            setActionsFor(null)
                            setEditing({ id: c.id, title: c.title })
                          }}
                          aria-label={t('history.rename')}
                          title={t('history.rename')}
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </button>
                        <button
                          className="rounded-md p-1.5 text-muted hover:text-danger"
                          onClick={() => remove(c)}
                          aria-label={t('history.delete')}
                          title={t('history.delete')}
                        >
                          <Trash className="h-3.5 w-3.5" />
                        </button>
                        <button
                          className="mr-1 rounded-md p-1.5 text-muted hover:text-white"
                          onClick={() => setActionsFor(null)}
                          aria-label={t('common.close')}
                        >
                          <X className="h-3.5 w-3.5" />
                        </button>
                      </>
                    ) : (
                      editing?.id !== c.id && (
                        // always visible on phones; on a computer it appears on hover
                        <button
                          className="mr-1 rounded-md p-1.5 text-muted hover:text-white md:opacity-0 md:group-hover:opacity-100 md:focus:opacity-100"
                          onClick={() => setActionsFor(c.id)}
                          aria-label={t('history.actions')}
                          title={t('history.actions')}
                        >
                          <Ellipsis className="h-4 w-4" />
                        </button>
                      )
                    )}
                    {editing?.id === c.id && (
                      <button
                        className="mr-1 rounded-md p-1.5 text-acid"
                        onMouseDown={(e) => e.preventDefault()}
                        onClick={saveTitle}
                        aria-label={t('admin.save')}
                      >
                        <Check className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                )
              })}
            </div>
          ))}
      </div>
    </div>
  )
}
