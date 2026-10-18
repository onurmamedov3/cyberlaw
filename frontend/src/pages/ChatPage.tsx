import {type KeyboardEvent, useEffect, useRef, useState} from 'react'
import {AnimatePresence, motion} from 'motion/react'
import {
    BookOpen,
    LoaderCircle,
    Plus,
    RefreshCw,
    ScanSearch,
    SendHorizontal,
    Sparkles,
    TriangleAlert
} from 'lucide-react'
import {useChat} from '../lib/chat'
import {useT} from '../lib/settings'
import type {TKey} from '../lib/i18n'
import type {ChatMessage, SearchScope, Source} from '../lib/types'
import {Markdown} from '../components/Markdown'
import {SourceDrawer} from '../components/SourceDrawer'

const SCOPES: SearchScope[] = ['OFFICIAL', 'MINE', 'BOTH']
const SUGGESTIONS: TKey[] = ['chat.suggest1', 'chat.suggest2', 'chat.suggest3', 'chat.suggest4']

export function ChatPage() {
  const t = useT()
  const { messages, scope, setScope, ask, busy, newChat } = useChat()
  const [input, setInput] = useState('')
  const [openSource, setOpenSource] = useState<Source | null>(null)
  const bottom = useRef<HTMLDivElement>(null)
  const textarea = useRef<HTMLTextAreaElement>(null)

  const lastText = messages[messages.length - 1]?.text
  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages.length, lastText])

  // grow the textarea with its content
  useEffect(() => {
    const el = textarea.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`
  }, [input])

  const send = (text = input) => {
    if (!text.trim() || busy) return
    ask(text)
    setInput('')
  }

  const onKey = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  return (
    <div className="mx-auto flex h-full max-w-4xl flex-col px-4">
      {/* header */}
      <div className="flex flex-wrap items-center justify-between gap-3 py-4">
        <div className="flex items-center gap-2">
          <span className="label-mono">{t('chat.scope')}</span>
          <div className="inline-flex rounded-xl border border-line bg-panel/60 p-1">
            {SCOPES.map((s) => (
              <button
                key={s}
                onClick={() => setScope(s)}
                className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
                  scope === s ? 'bg-neon/15 text-neon shadow-[inset_0_0_0_1px_rgb(34_211_238/0.5)]' : 'text-muted hover:text-white'
                }`}
              >
                {t(`chat.scope.${s}`)}
              </button>
            ))}
          </div>
        </div>
        {messages.length > 0 && (
          <button className="btn btn-ghost py-2 text-xs" onClick={newChat}>
            <Plus className="h-4 w-4" /> {t('chat.newChat')}
          </button>
        )}
      </div>

      {/* messages */}
      <div className="flex-1 space-y-6 pb-6">
        {messages.length === 0 ? (
          <EmptyState onPick={(q) => send(q)} />
        ) : (
          messages.map((m) =>
            m.role === 'user' ? (
              <UserBubble key={m.id} text={m.text} />
            ) : (
              <AssistantMessage key={m.id} message={m} onOpenSource={setOpenSource} />
            ),
          )
        )}
        <div ref={bottom} />
      </div>

      {/* composer */}
      <div className="sticky bottom-0 pb-4 pt-2">
        <div className="glass flex items-end gap-2 p-2 shadow-[0_0_40px_-18px_var(--color-neon)] focus-within:border-neon/50">
          <textarea
            ref={textarea}
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKey}
            placeholder={t('chat.placeholder')}
            maxLength={2000}
            className="max-h-[200px] flex-1 resize-none bg-transparent px-3 py-2.5 text-[15px] outline-none placeholder:text-muted/70"
          />
          <button className="btn btn-neon h-11 w-11 shrink-0 p-0" disabled={busy || !input.trim()} onClick={() => send()} aria-label={t('chat.send')}>
            {busy ? <LoaderCircle className="h-5 w-5 animate-spin" /> : <SendHorizontal className="h-5 w-5" />}
          </button>
        </div>
        <p className="mt-2 text-center text-[11px] text-muted">{t('chat.disclaimer')}</p>
      </div>

      <SourceDrawer source={openSource} onClose={() => setOpenSource(null)} />
    </div>
  )
}

function EmptyState({ onPick }: { onPick: (q: string) => void }) {
  const t = useT()
  return (
    <motion.div className="pt-10 text-center sm:pt-20" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
      <div className="mx-auto mb-6 grid h-20 w-20 place-items-center rounded-2xl border border-neon/40 bg-neon/10 shadow-[0_0_50px_-10px_var(--color-neon)]">
        <Sparkles className="h-10 w-10 text-neon" />
      </div>
      <h1 className="font-display text-3xl font-extrabold tracking-wide sm:text-4xl">
        <span className="neon-text">{t('chat.emptyTitle')}</span>
      </h1>
      <p className="mx-auto mt-3 max-w-lg text-muted">{t('chat.emptySubtitle')}</p>
      <div className="mt-10 grid gap-3 text-left sm:grid-cols-2">
        {SUGGESTIONS.map((key, i) => (
          <motion.button
            key={key}
            onClick={() => onPick(t(key))}
            className="glass glass-hover p-4 text-sm text-slate-300"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 + i * 0.08 }}
          >
            <span className="mb-1 block font-mono text-xs text-neon">&gt; query_{String(i + 1).padStart(2, '0')}</span>
            {t(key)}
          </motion.button>
        ))}
      </div>
    </motion.div>
  )
}

function UserBubble({ text }: { text: string }) {
  return (
    <motion.div className="flex justify-end" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
      <div className="max-w-[85%] whitespace-pre-wrap rounded-2xl rounded-br-md border border-violet/40 bg-violet/15 px-4 py-3 text-[15px] shadow-[0_0_24px_-12px_var(--color-violet)]">
        {text}
      </div>
    </motion.div>
  )
}

function AssistantMessage({ message, onOpenSource }: { message: ChatMessage; onOpenSource: (s: Source) => void }) {
  const t = useT()
  const { retry, retryCountdown } = useChat()
  const sources = message.sources ?? []
  const openIndex = (i: number) => {
    const s = sources.find((x) => x.index === i)
    if (s) onOpenSource(s)
  }

  return (
    <motion.div className="flex gap-3" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
      <div className="mt-1 grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-neon/40 bg-neon/10">
        <Sparkles className="h-4 w-4 text-neon" />
      </div>
      <div className="min-w-0 flex-1">
        <AnimatePresence>
          {message.status === 'searching' && (
            <motion.div
              className="scan-bar glass inline-flex items-center gap-3 px-4 py-3 font-mono text-sm text-neon"
              exit={{ opacity: 0, height: 0 }}
            >
              <ScanSearch className="h-4 w-4 animate-pulse" />
              {t('chat.searching')}
              <span className="typing-cursor" />
            </motion.div>
          )}
        </AnimatePresence>

        {sources.length > 0 && message.status !== 'searching' && (
          <div className="label-mono mb-2 text-acid">
            ✓ {sources.length} {t('chat.foundSources')}
          </div>
        )}

        {message.text && (
          <div>
            <Markdown text={message.text} onCite={openIndex} />
            {message.status === 'streaming' && <span className="typing-cursor" />}
          </div>
        )}

        {message.status === 'error' && (
          <div className="mt-2 flex flex-wrap items-center gap-3 rounded-xl border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-red-200">
            <TriangleAlert className="h-4 w-4 shrink-0" />
            <span className="flex-1">
              {message.errorCode === 'RATE_LIMIT' && retryCountdown !== null
                ? `${t('chat.retryIn')} ${retryCountdown}${t('chat.seconds')}`
                : message.errorMessage}
            </span>
            <button className="btn btn-danger py-1.5 text-xs" onClick={() => retry(message.id)}>
              <RefreshCw className="h-3.5 w-3.5" /> {t('chat.retry')}
            </button>
          </div>
        )}

        {sources.length > 0 && message.status === 'done' && (
          <div className="mt-4">
            <div className="label-mono mb-2">{t('chat.sources')}</div>
            <div className="grid gap-2 sm:grid-cols-2">
              {sources.map((s) => (
                <button
                  key={s.index}
                  onClick={() => onOpenSource(s)}
                  className="glass glass-hover flex items-start gap-3 p-3 text-left"
                >
                  <span className="cite mt-0.5">{s.index}</span>
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium">{s.documentTitle}</span>
                    <span className="flex items-center gap-1 truncate text-xs text-muted">
                      {s.article ? (
                        <>
                          <BookOpen className="h-3 w-3 shrink-0" />
                          {s.article}
                          {s.articleTitle && ` · ${s.articleTitle}`}
                        </>
                      ) : (
                        s.text.slice(0, 60) + '…'
                      )}
                    </span>
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </motion.div>
  )
}
