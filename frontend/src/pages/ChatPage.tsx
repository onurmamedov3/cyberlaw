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
    Scale,
    Square,
    TriangleAlert
} from 'lucide-react'
import {useChat} from '../lib/chat'
import {copyText} from '../lib/clipboard'
import {useAppConfig} from '../lib/config'
import {useErrorText} from '../lib/errors'
import {useT} from '../lib/settings'
import type {TKey} from '../lib/i18n'
import type {ChatMessage, Source} from '../lib/types'
import {Markdown, citedIndexes} from '../components/Markdown'
import {ScopePicker} from '../components/ScopePicker'
import {SourceDrawer} from '../components/SourceDrawer'
import {ErrorNote} from '../components/ErrorNote'

const SUGGESTIONS: { key: TKey; icon: typeof FileText }[] = [
  { key: 'chat.suggest1', icon: FileText },
  { key: 'chat.suggest2', icon: LockKeyhole },
  { key: 'chat.suggest3', icon: ShieldCheck },
  { key: 'chat.suggest4', icon: TriangleAlert },
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
    <div ref={page} className="flex min-h-[calc(100%-4.25rem)] flex-col">
      <div className="chat-content mx-auto flex w-full flex-1 flex-col">
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

        {/* The search scope and send control sit below the writing area. */}
        <div className="chat-composer">
          <div className="composer-box">
            <div>
              <textarea
                ref={textarea}
                rows={1}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={onKey}
                placeholder={t('chat.placeholder')}
                maxLength={config?.maxQuestionLength}
                aria-label={t('chat.placeholder')}
                className="chat-textarea"
              />
              <div className="flex items-end justify-between gap-3 px-3 pb-3">
              <ScopePicker scope={scope} onChange={setScope} />
              {/* while an answer is being written, the send button stops it */}
              <button
                className="composer-send shrink-0"
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
          </div>
          <p className="mt-3 flex items-center justify-center gap-2 text-center text-xs text-muted">
            <ShieldCheck className="h-4 w-4 shrink-0 text-muted" />
            {t('chat.disclaimer')}
          </p>
        </div>
      </div>

      <SourceDrawer source={openSource} onClose={() => setOpenSource(null)} />
    </div>
  )
}

function EmptyState({ onPick }: { onPick: (q: string) => void }) {
  const t = useT()
  return (
    <motion.div className="chat-empty" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
      <div className="label-mono mb-6 flex items-center gap-2 text-neon">
        <BookOpen className="h-4 w-4" strokeWidth={1.5} />
        {t('workspace.research')}
      </div>
      <h1>{t('chat.emptyTitle')}</h1>
      <p className="mt-5 max-w-lg text-[15px] leading-relaxed text-muted">{t('chat.emptySubtitle')}</p>
      <div className="label-mono mt-12">{t('workspace.start')}</div>
      <div className="chat-suggestions">
        {SUGGESTIONS.map(({ key, icon: Icon }, i) => (
          <motion.button
            key={key}
            onClick={() => onPick(t(key))}
            className="chat-suggestion group"
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.05 + i * 0.04 }}
          >
            <Icon className="mt-0.5 h-[18px] w-[18px] shrink-0 text-neon" strokeWidth={1.5} />
            <span className="flex-1 text-[13px] leading-relaxed">{t(key)}</span>
            <ArrowRight className="mt-1 h-3.5 w-3.5 shrink-0 text-muted transition-transform group-hover:translate-x-0.5" />
          </motion.button>
        ))}
      </div>
    </motion.div>
  )
}

function UserBubble({ text }: { text: string }) {
  return (
    <motion.div className="flex justify-end" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
      <div className="max-w-[85%] whitespace-pre-wrap rounded-lg rounded-br-sm border border-neon/15 bg-neon/5 px-4 py-3 text-[15px]">
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
        <Scale className="h-4 w-4 text-neon" />
      </div>
      <div className="min-w-0 flex-1">
        <AnimatePresence>
          {thinking && (
            <motion.div
              className="scan-bar glass inline-flex items-center gap-3 px-4 py-3 font-mono text-sm text-neon"
              exit={{ opacity: 0, height: 0 }}
            >
              <Scale className="h-4 w-4 animate-pulse" />
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

        {message.status === 'error' && message.errorCode === 'BLOCKED' && (
          // the safety filter stopped the answer: a calm notice without Retry, asking again would be blocked again
          <div className="mt-2 flex items-center gap-3 rounded-xl border border-amber/30 bg-amber/5 px-4 py-3 text-sm text-amber">
            <ShieldCheck className="h-4 w-4 shrink-0 text-amber" />
            <span className="flex-1">{errorText({ code: message.errorCode, message: message.errorMessage })}</span>
          </div>
        )}

        {message.status === 'error' && message.errorCode !== 'BLOCKED' && (
          <div className="mt-2 flex flex-wrap items-center gap-3 rounded-xl border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-danger">
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
    'inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs text-muted transition-colors hover:bg-neon/5 hover:text-ink disabled:opacity-40 disabled:hover:bg-transparent'
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
