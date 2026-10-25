import {type KeyboardEvent, useEffect, useRef, useState} from 'react'
import {useNavigate, useParams} from 'react-router-dom'
import {AnimatePresence, motion} from 'motion/react'
import {
    ArrowRight,
    BookOpen,
    Check,
    ChevronRight,
    Copy,
    LoaderCircle,
    FileText,
    LockKeyhole,
    RefreshCw,
    RotateCcw,
    ShieldCheck,
    Sparkles,
    Square,
    TriangleAlert
} from 'lucide-react'
import {useChat} from '../lib/chat'
import {copyText} from '../lib/clipboard'
import {useAppConfig} from '../lib/config'
import {useErrorText} from '../lib/errors'
import {useSettings, useT} from '../lib/settings'
import type {TKey} from '../lib/i18n'
import type {ChatMessage, Source} from '../lib/types'
import {Markdown, citedIndexes} from '../components/Markdown'
import {ScopePicker} from '../components/ScopePicker'
import {SourceDrawer} from '../components/SourceDrawer'
import {ErrorNote} from '../components/ErrorNote'

/** Suggestion cards; class names are written out in full so Tailwind can find them. */
const SUGGESTIONS: { key: TKey; icon: typeof FileText; tile: string; hover: string }[] = [
  {
    key: 'chat.suggest1',
    icon: FileText,
    tile: 'bg-sky-500/15 text-sky-300',
    hover: 'hover:border-sky-400/50 hover:shadow-[0_0_32px_-10px_rgb(56_189_248/0.8)]',
  },
  {
    key: 'chat.suggest2',
    icon: LockKeyhole,
    tile: 'bg-violet-500/15 text-violet-300',
    hover: 'hover:border-violet-400/50 hover:shadow-[0_0_32px_-10px_rgb(168_85_247/0.8)]',
  },
  {
    key: 'chat.suggest3',
    icon: ShieldCheck,
    tile: 'bg-emerald-500/15 text-emerald-300',
    hover: 'hover:border-emerald-400/50 hover:shadow-[0_0_32px_-10px_rgb(52_211_153/0.8)]',
  },
  {
    key: 'chat.suggest4',
    icon: TriangleAlert,
    tile: 'bg-amber-500/15 text-amber-300',
    hover: 'hover:border-amber-400/50 hover:shadow-[0_0_32px_-10px_rgb(251_191_36/0.8)]',
  },
]

export function ChatPage() {
  const t = useT()
  const { messages, ask, busy, stop, scope, setScope, conversationId, opening, loadError, openConversation, newChat } =
    useChat()
  const { id } = useParams()
  const navigate = useNavigate()

  // The URL decides what is on screen: /chat/<id> is a saved chat, /chat a new one.
  useEffect(() => {
    if (id) openConversation(id)
    else if (conversationId) newChat()
    // only when the URL changes; conversationId is read as it is at that moment
  }, [id])

  // A new chat gets its own address once its first answer is saved.
  const previousConversation = useRef(conversationId)
  useEffect(() => {
    if (!id && conversationId && previousConversation.current === null) {
      navigate(`/chat/${conversationId}`, { replace: true })
    }
    previousConversation.current = conversationId
  }, [conversationId, id, navigate])
  const config = useAppConfig()
  const [input, setInput] = useState('')
  const [openSource, setOpenSource] = useState<Source | null>(null)
  const page = useRef<HTMLDivElement>(null)
  const textarea = useRef<HTMLTextAreaElement>(null)

  // Follow the answer to the very end of <main> (so nothing hides behind the sticky composer),
  // unless the user scrolls up to read; scrolling back to the end resumes following.
  const following = useRef(true)
  useEffect(() => {
    const main = page.current?.closest('main')
    if (!main) return
    let lastTop = main.scrollTop
    const onScroll = () => {
      if (main.scrollTop < lastTop - 2) following.current = false
      if (main.scrollHeight - main.scrollTop - main.clientHeight < 40) following.current = true
      lastTop = main.scrollTop
    }
    main.addEventListener('scroll', onScroll, { passive: true })
    return () => main.removeEventListener('scroll', onScroll)
  }, [])

  const last = messages[messages.length - 1]
  const count = useRef(messages.length)
  useEffect(() => {
    if (messages.length !== count.current) following.current = true // a new question always jumps down
    count.current = messages.length
    const main = page.current?.closest('main')
    // the empty page stays at the top, so its title is not scrolled away on phones
    if (main && following.current && messages.length > 0) main.scrollTo({ top: main.scrollHeight, behavior: 'smooth' })
  }, [messages.length, last?.text, last?.status])

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
    <div ref={page} className="flex min-h-full flex-col">
      <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col px-4">
        {/* messages */}
        <div className="flex-1 space-y-6 py-6">
          <ErrorNote error={loadError} />
          {opening ? (
            <div className="grid place-items-center pt-24 text-muted">
              <LoaderCircle className="h-8 w-8 animate-spin text-neon" />
            </div>
          ) : messages.length === 0 ? (
            <EmptyState onPick={(q) => send(q)} />
          ) : (
            messages.map((m) =>
              m.role === 'user' ? (
                <UserBubble key={m.id} text={m.text} />
              ) : (
                <AssistantMessage
                  key={m.id}
                  message={m}
                  last={m === messages[messages.length - 1]}
                  onOpenSource={setOpenSource}
                />
              ),
            )
          )}
        </div>

        {/* composer: pill with a cyan→violet gradient border */}
        <div className="sticky bottom-0 pb-4 pt-6">
          <div className="rounded-[30px] bg-gradient-to-r from-neon via-sky-500 to-violet p-[1.5px] shadow-[0_0_40px_-12px_rgb(56_189_248/0.6)] transition-shadow focus-within:shadow-[0_0_50px_-8px_rgb(168_85_247/0.7)]">
            <div className="flex items-end gap-2 rounded-[28.5px] bg-[#070b16] py-2 pl-2.5 pr-2 sm:gap-3">
              <ScopePicker scope={scope} onChange={setScope} />
              <textarea
                ref={textarea}
                rows={1}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={onKey}
                placeholder={t('chat.placeholder')}
                maxLength={config?.maxQuestionLength}
                className="max-h-[200px] flex-1 resize-none bg-transparent py-3 text-[15px] outline-none placeholder:text-muted/80"
              />
              {/* while an answer is being written, the send button stops it */}
              <button
                className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-gradient-to-br from-sky-400 via-blue-500 to-violet-500 text-white shadow-[0_0_22px_-4px_rgb(56_189_248/0.8)] transition-all hover:scale-105 disabled:opacity-40 disabled:hover:scale-100"
                disabled={!busy && !input.trim()}
                onClick={() => (busy ? stop() : send())}
                aria-label={busy ? t('chat.stop') : t('chat.send')}
                title={busy ? t('chat.stop') : t('chat.send')}
              >
                {busy ? (
                  <Square className="h-4 w-4 fill-current" />
                ) : (
                  <ChevronRight className="h-6 w-6" strokeWidth={2.5} />
                )}
              </button>
            </div>
          </div>
          <p className="mt-3 flex items-center justify-center gap-2 text-center text-xs text-muted">
            <ShieldCheck className="h-4 w-4 shrink-0 text-violet-400" />
            {t('chat.disclaimer')}
          </p>
        </div>
      </div>

      <SourceDrawer source={openSource} onClose={() => setOpenSource(null)} />
    </div>
  )
}

/** Small dots floating around the hero icon. */
const DOTS = [
  { x: -175, y: 12, size: 6, color: 'bg-neon' },
  { x: -130, y: -18, size: 4, color: 'bg-sky-500' },
  { x: 125, y: -18, size: 5, color: 'bg-violet' },
  { x: 95, y: 8, size: 4, color: 'bg-indigo-400' },
  { x: 160, y: 22, size: 5, color: 'bg-violet-500' },
]

function EmptyState({ onPick }: { onPick: (q: string) => void }) {
  const { t, animations } = useSettings()
  return (
    <motion.div className="pt-6 text-center sm:pt-12" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
      <div className="relative mx-auto mb-8 h-24 w-24">
        {DOTS.map((d, i) => (
          <motion.span
            key={i}
            className={`absolute left-1/2 top-1/2 rounded-full ${d.color}`}
            style={{ width: d.size, height: d.size, marginLeft: d.x, marginTop: d.y }}
            animate={animations ? { y: [0, -7, 0], opacity: [0.45, 1, 0.45] } : undefined}
            transition={{ duration: 3 + i * 0.6, repeat: Infinity, ease: 'easeInOut' }}
          />
        ))}
        <div className="rounded-3xl bg-gradient-to-br from-neon to-violet p-[1.5px] shadow-[0_0_60px_-8px_rgb(56_189_248/0.7)]">
          <div className="grid h-[93px] w-[93px] place-items-center rounded-[22.5px] bg-panel">
            <Sparkles color="url(#cl-gradient)" className="h-12 w-12" strokeWidth={1.6} />
          </div>
        </div>
      </div>

      <h1 className="text-5xl font-extrabold tracking-tight sm:text-6xl">
        <span className="neon-text">{t('chat.emptyTitle')}</span>
      </h1>
      <p className="mx-auto mt-4 max-w-xl text-lg text-slate-300">{t('chat.emptySubtitle')}</p>

      <div className="mt-12 grid gap-4 text-left md:grid-cols-2">
        {SUGGESTIONS.map(({ key, icon: Icon, tile, hover }, i) => (
          <motion.button
            key={key}
            onClick={() => onPick(t(key))}
            className={`group glass flex items-center gap-4 rounded-2xl p-4 transition-all ${hover}`}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 + i * 0.08 }}
          >
            <span className={`grid h-16 w-16 shrink-0 place-items-center rounded-xl ${tile}`}>
              <Icon className="h-8 w-8" strokeWidth={1.75} />
            </span>
            <span className="min-w-0 flex-1 text-[15px] leading-snug text-slate-100">{t(key)}</span>
            <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-line text-slate-300 transition-all group-hover:translate-x-0.5 group-hover:border-white/30 group-hover:text-white">
              <ArrowRight className="h-4 w-4" />
            </span>
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

function AssistantMessage({
  message,
  last,
  onOpenSource,
}: {
  message: ChatMessage
  last: boolean
  onOpenSource: (s: Source) => void
}) {
  const t = useT()
  const errorText = useErrorText()
  const { busy, retry, regenerate, countdown } = useChat()
  const sources = message.sources ?? []
  // only the sources the finished answer cites (retrieval may return unused passages)
  const cited = citedIndexes(message.text)
  const shown = message.status === 'done' ? sources.filter((s) => cited.has(s.index)) : []
  // searching the documents and waiting for the first words of the answer
  const thinking = message.status === 'searching' || (message.status === 'streaming' && !message.text)
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
          {thinking && (
            <motion.div
              className="scan-bar glass inline-flex items-center gap-3 px-4 py-3 font-mono text-sm text-neon"
              exit={{ opacity: 0, height: 0 }}
            >
              <Sparkles className="h-4 w-4 animate-pulse" />
              {t('chat.thinking')}
              <span className="typing-cursor" />
            </motion.div>
          )}
        </AnimatePresence>

        {message.text && (
          <div>
            <Markdown text={message.text} onCite={openIndex} />
            {message.status === 'streaming' && <span className="typing-cursor" />}
          </div>
        )}

        {message.stopped && <p className="mt-2 text-xs italic text-muted">{t('chat.stopped')}</p>}

        {message.status === 'error' && (
          <div className="mt-2 flex flex-wrap items-center gap-3 rounded-xl border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-red-200">
            <TriangleAlert className="h-4 w-4 shrink-0" />
            <span className="flex-1">
              {countdown?.id === message.id
                ? t(message.errorCode === 'TOO_MANY_QUESTIONS' ? 'chat.tooFastRetryIn' : 'chat.retryIn', {
                    seconds: countdown.seconds,
                  })
                : errorText({ code: message.errorCode, message: message.errorMessage, params: message.errorParams })}
            </span>
            <button className="btn btn-danger py-1.5 text-xs" onClick={() => retry(message.id)}>
              <RefreshCw className="h-3.5 w-3.5" /> {t('chat.retry')}
            </button>
          </div>
        )}

        {shown.length > 0 && (
          <div className="mt-4">
            <div className="label-mono mb-2">
              {t('chat.sources')} · {shown.length}
            </div>
            <div className="grid gap-2 sm:grid-cols-2">
              {shown.map((s) => (
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
                        s.deleted ? t('source.deletedShort') : s.text.slice(0, 60) + '…'
                      )}
                    </span>
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}

        {message.status === 'done' && (
          <AnswerActions
            text={message.text}
            onRegenerate={last ? () => regenerate(message.id) : undefined}
            disabled={busy}
          />
        )}
      </div>
    </motion.div>
  )
}

/** Copy, and Regenerate for the latest answer. */
function AnswerActions({
  text,
  onRegenerate,
  disabled,
}: {
  text: string
  onRegenerate?: () => void
  disabled: boolean
}) {
  const t = useT()
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    await copyText(text)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 1500)
  }

  const action =
    'inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs text-muted transition-colors hover:bg-white/5 hover:text-white disabled:opacity-40 disabled:hover:bg-transparent'
  return (
    <div className="mt-3 flex items-center gap-1">
      {text && (
        <button className={action} onClick={copy}>
          {copied ? <Check className="h-3.5 w-3.5 text-acid" /> : <Copy className="h-3.5 w-3.5" />}
          {copied ? t('chat.copied') : t('chat.copy')}
        </button>
      )}
      {onRegenerate && (
        <button className={action} onClick={onRegenerate} disabled={disabled}>
          <RotateCcw className="h-3.5 w-3.5" />
          {t('chat.regenerate')}
        </button>
      )}
    </div>
  )
}
