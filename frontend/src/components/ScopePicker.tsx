import {useEffect, useRef, useState} from 'react'
import {AnimatePresence, motion} from 'motion/react'
import {Blend, Check, ChevronDown, FileText, Landmark} from 'lucide-react'
import {useT} from '../lib/settings'
import type {SearchScope} from '../lib/types'

const SCOPES: { scope: SearchScope; icon: typeof Landmark }[] = [
  { scope: 'OFFICIAL', icon: Landmark },
  { scope: 'MINE', icon: FileText },
  { scope: 'BOTH', icon: Blend },
]

/** Which documents the chat searches, chosen from a small menu inside the message box. */
export function ScopePicker({ scope, onChange }: { scope: SearchScope; onChange: (scope: SearchScope) => void }) {
  const t = useT()
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const Current = SCOPES.find((s) => s.scope === scope)?.icon ?? Blend

  // a click outside or Escape closes the menu
  useEffect(() => {
    if (!open) return
    const onClick = (e: MouseEvent) => !root.current?.contains(e.target as Node) && setOpen(false)
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  return (
    <div ref={root} className="relative shrink-0">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`${t('chat.scope')}: ${t(`chat.scope.${scope}`)}`}
        title={`${t('chat.scope')}: ${t(`chat.scope.${scope}`)}`}
        className="flex h-9 items-center gap-2 rounded-md border border-line bg-void/70 px-2.5 text-xs font-medium text-ink transition-colors hover:border-neon/50 hover:text-ink"
      >
        <Current className="h-4.5 w-4.5 text-neon" strokeWidth={1.9} />
        <span className="hidden sm:inline">{t(`chat.scope.${scope}`)}</span>
        <ChevronDown className={`h-3.5 w-3.5 text-muted transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            role="menu"
            className="glass absolute bottom-full left-0 z-30 mb-3 w-[min(20rem,calc(100vw-3rem))] p-1.5"
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 6 }}
          >
            <div className="label-mono px-3 pb-1 pt-2">{t('chat.scope')}</div>
            {SCOPES.map(({ scope: s, icon: Icon }) => (
              <button
                key={s}
                type="button"
                role="menuitemradio"
                aria-checked={scope === s}
                onClick={() => {
                  onChange(s)
                  setOpen(false)
                }}
                className={`flex w-full items-start gap-3 rounded-xl px-3 py-2.5 text-left transition-colors ${
                  scope === s ? 'bg-neon/10' : 'hover:bg-neon/5'
                }`}
              >
                <Icon className={`mt-0.5 h-4.5 w-4.5 shrink-0 ${scope === s ? 'text-neon' : 'text-muted'}`} />
                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-semibold">{t(`chat.scope.${s}`)}</span>
                  <span className="block text-xs text-muted">{t(`chat.scopeHint.${s}`)}</span>
                </span>
                {scope === s && <Check className="mt-0.5 h-4 w-4 shrink-0 text-neon" />}
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
