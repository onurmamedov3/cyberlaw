import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState} from 'react'
import {type ApiError, del, get, patch, streamChat} from './api'
import {useSettings} from './settings'
import type {ChatMessage, Conversation, ConversationDetail, SearchScope} from './types'

const RETRY_SECONDS = 20

/** Seconds before the one automatic retry: Gemini's free-tier limit, or our own per-user limit. */
function autoRetrySeconds(error: ApiError) {
  if (error.code === 'RATE_LIMIT') return RETRY_SECONDS
  if (error.code === 'TOO_MANY_QUESTIONS') return Math.max(1, Number(error.params.seconds) || RETRY_SECONDS)
  return null
}

interface ChatContextValue {
  messages: ChatMessage[]
  scope: SearchScope
  busy: boolean
  /** The answer that will be retried automatically, and in how many seconds. */
  countdown: { id: string; seconds: number } | null
  setScope: (scope: SearchScope) => void
  ask: (question: string) => void
  /** Stops the answer being written; what has arrived so far stays. */
  stop: () => void
  /** Asks again after an error. */
  retry: (messageId: string) => void
  /** A new answer to the last question, replacing the previous one. */
  regenerate: (messageId: string) => void

  /** The saved chat on screen; null for a new chat until its first answer is saved. */
  conversationId: string | null
  /** The user's saved chats, newest first; null while loading. */
  conversations: Conversation[] | null
  /** A saved chat is being opened. */
  opening: boolean
  /** Opening a chat or loading the list failed. */
  loadError: unknown
  newChat: () => void
  openConversation: (id: string) => void
  renameConversation: (id: string, title: string) => Promise<void>
  deleteConversation: (id: string) => Promise<void>
  deleteAllConversations: () => Promise<void>
}

const ChatContext = createContext<ChatContextValue | null>(null)

const newId = () => crypto.randomUUID()

/** A saved chat as chat messages; an answer knows its question, for Regenerate. */
function toMessages(detail: ConversationDetail): ChatMessage[] {
  let question = ''
  return detail.messages.map((m) => {
    if (m.role === 'user') question = m.text
    return m.role === 'user'
      ? { id: m.id, role: 'user', text: m.text, status: 'done' }
      : { id: m.id, role: 'assistant', text: m.text, sources: m.sources, status: 'done', question }
  })
}

/**
 * Chat state lives above the router, so an answer keeps streaming while the user looks at another page
 * or another chat. An answer is saved by the server when it is complete, so switching away loses nothing.
 */
export function ChatProvider({ children }: { children: ReactNode }) {
  const { lang } = useSettings()
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [scope, setScope] = useState<SearchScope>('BOTH')
  const [countdown, setCountdown] = useState<ChatContextValue['countdown']>(null)
  const [conversationId, setConversationIdState] = useState<string | null>(null)
  const [conversations, setConversations] = useState<Conversation[] | null>(null)
  const [opening, setOpening] = useState(false)
  const [loadError, setLoadError] = useState<unknown>(null)
  const conversationRef = useRef<string | null>(null)
  // changes whenever another chat is put on screen, so late answers of the previous one don't land here
  const session = useRef(0)
  const abort = useRef<AbortController | null>(null)
  const activeId = useRef<string | null>(null)
  const retryTimer = useRef<number | null>(null)
  const autoRetried = useRef(new Set<string>())

  const busy = messages.some((m) => m.status === 'searching' || m.status === 'streaming')

  const setConversationId = (id: string | null) => {
    conversationRef.current = id
    setConversationIdState(id)
  }

  const update = (id: string, patch: (m: ChatMessage) => Partial<ChatMessage>) =>
    setMessages((all) => all.map((m) => (m.id === id ? { ...m, ...patch(m) } : m)))

  useEffect(() => {
    get<Conversation[]>('/chat/conversations').then(setConversations).catch(setLoadError)
  }, [])

  const run = useCallback(
    (assistantId: string, question: string, regenerate = false) => {
      const controller = new AbortController()
      const mySession = session.current
      abort.current = controller
      activeId.current = assistantId
      streamChat(
        { question, conversationId: conversationRef.current, scope, language: lang, regenerate },
        {
          onSources: (sources) => update(assistantId, () => ({ sources, status: 'streaming' })),
          onToken: (text) => update(assistantId, (m) => ({ text: m.text + text, status: 'streaming' })),
          onConversation: (saved) => {
            setConversations((all) => [saved, ...(all ?? []).filter((c) => c.id !== saved.id)])
            if (mySession === session.current) setConversationId(saved.id)
          },
          onDone: () => update(assistantId, () => ({ status: 'done' })),
          onError: (error) => {
            update(assistantId, () => ({
              status: 'error',
              errorCode: error.code,
              errorMessage: error.message,
              errorParams: error.params,
            }))
            // only a few requests per minute are allowed: retry once automatically
            const seconds = autoRetrySeconds(error)
            if (seconds !== null && mySession === session.current && !autoRetried.current.has(assistantId)) {
              autoRetried.current.add(assistantId)
              startCountdown(assistantId, seconds)
            }
          },
        },
        controller.signal,
      )
    },
    [scope, lang],
  )

  const startCountdown = (assistantId: string, seconds: number) => {
    stopCountdown()
    let left = seconds
    setCountdown({ id: assistantId, seconds: left })
    retryTimer.current = window.setInterval(() => {
      left -= 1
      setCountdown({ id: assistantId, seconds: left })
      if (left <= 0) {
        stopCountdown()
        retryRef.current(assistantId)
      }
    }, 1000)
  }

  const stopCountdown = () => {
    if (retryTimer.current) window.clearInterval(retryTimer.current)
    retryTimer.current = null
    setCountdown(null)
  }

  const ask = useCallback(
    (raw: string) => {
      const question = raw.trim()
      if (!question || busy) return
      const assistantId = newId()
      setMessages((all) => [
        ...all,
        { id: newId(), role: 'user', text: question, status: 'done' },
        { id: assistantId, role: 'assistant', text: '', status: 'searching', question },
      ])
      run(assistantId, question)
    },
    [busy, run],
  )

  const messagesRef = useRef(messages)
  messagesRef.current = messages

  const askAgain = useCallback(
    (messageId: string, regenerate: boolean) => {
      stopCountdown()
      const question = messagesRef.current.find((m) => m.id === messageId)?.question
      if (!question) return
      update(messageId, () => ({
        text: '',
        sources: undefined,
        status: 'searching',
        stopped: undefined,
        errorCode: undefined,
        errorMessage: undefined,
        errorParams: undefined,
      }))
      run(messageId, question, regenerate)
    },
    [run],
  )
  const retry = useCallback((messageId: string) => askAgain(messageId, false), [askAgain])
  const regenerate = useCallback((messageId: string) => askAgain(messageId, true), [askAgain])
  const retryRef = useRef(retry)
  retryRef.current = retry

  const stop = useCallback(() => {
    abort.current?.abort()
    const id = activeId.current
    if (id) {
      update(id, (m) => (m.status === 'searching' || m.status === 'streaming' ? { status: 'done', stopped: true } : {}))
    }
  }, [])

  /** An empty chat. An answer still being written for the previous chat is finished and saved in the background. */
  const newChat = useCallback(() => {
    session.current++
    stopCountdown()
    autoRetried.current.clear()
    setConversationId(null)
    setMessages([])
    setOpening(false)
    setLoadError(null)
  }, [])

  const openConversation = useCallback(async (id: string) => {
    if (id === conversationRef.current) return
    const mySession = ++session.current
    stopCountdown()
    setConversationId(id)
    setMessages([])
    setOpening(true)
    setLoadError(null)
    try {
      const detail = await get<ConversationDetail>(`/chat/conversations/${id}`)
      if (mySession !== session.current) return
      setScope(detail.conversation.scope)
      setMessages(toMessages(detail))
    } catch (e) {
      if (mySession === session.current) setLoadError(e)
    } finally {
      if (mySession === session.current) setOpening(false)
    }
  }, [])

  const renameConversation = useCallback(async (id: string, title: string) => {
    const renamed = await patch<Conversation>(`/chat/conversations/${id}`, { title })
    setConversations((all) => all?.map((c) => (c.id === id ? renamed : c)) ?? null)
  }, [])

  const deleteConversation = useCallback(
    async (id: string) => {
      await del(`/chat/conversations/${id}`)
      setConversations((all) => all?.filter((c) => c.id !== id) ?? null)
      if (id === conversationRef.current) newChat()
    },
    [newChat],
  )

  const deleteAllConversations = useCallback(async () => {
    await del('/chat/conversations')
    setConversations([])
    newChat()
  }, [newChat])

  useEffect(() => () => stopCountdown(), [])

  const value = useMemo(
    () => ({
      messages,
      scope,
      busy,
      countdown,
      setScope,
      ask,
      stop,
      retry,
      regenerate,
      conversationId,
      conversations,
      opening,
      loadError,
      newChat,
      openConversation,
      renameConversation,
      deleteConversation,
      deleteAllConversations,
    }),
    [
      messages,
      scope,
      busy,
      countdown,
      ask,
      stop,
      retry,
      regenerate,
      conversationId,
      conversations,
      opening,
      loadError,
      newChat,
      openConversation,
      renameConversation,
      deleteConversation,
      deleteAllConversations,
    ],
  )
  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>
}

export function useChat() {
  const ctx = useContext(ChatContext)
  if (!ctx) throw new Error('useChat must be used inside ChatProvider')
  return ctx
}
