import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState} from 'react'
import {del, streamChat} from './api'
import {useSettings} from './settings'
import type {ChatMessage, SearchScope} from './types'

const RETRY_SECONDS = 20

interface ChatContextValue {
  messages: ChatMessage[]
  scope: SearchScope
  busy: boolean
  retryCountdown: number | null
  setScope: (scope: SearchScope) => void
  ask: (question: string) => void
  retry: (messageId: string) => void
  newChat: () => void
}

const ChatContext = createContext<ChatContextValue | null>(null)

const newId = () => crypto.randomUUID()

/**
 * Chat state lives above the router, so an answer keeps streaming
 * while the user looks at another page.
 */
export function ChatProvider({ children }: { children: ReactNode }) {
  const { lang } = useSettings()
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [scope, setScope] = useState<SearchScope>('BOTH')
  const [retryCountdown, setRetryCountdown] = useState<number | null>(null)
  const conversationId = useRef(newId())
  const abort = useRef<AbortController | null>(null)
  const retryTimer = useRef<number | null>(null)
  const autoRetried = useRef(new Set<string>())

  const busy = messages.some((m) => m.status === 'searching' || m.status === 'streaming')

  const update = (id: string, patch: (m: ChatMessage) => Partial<ChatMessage>) =>
    setMessages((all) => all.map((m) => (m.id === id ? { ...m, ...patch(m) } : m)))

  const run = useCallback(
    (assistantId: string, question: string) => {
      const controller = new AbortController()
      abort.current = controller
      streamChat(
        { question, conversationId: conversationId.current, scope, language: lang },
        {
          onSources: (sources) => update(assistantId, () => ({ sources, status: 'streaming' })),
          onToken: (text) => update(assistantId, (m) => ({ text: m.text + text, status: 'streaming' })),
          onDone: () => update(assistantId, () => ({ status: 'done' })),
          onError: (code, message) => {
            update(assistantId, () => ({ status: 'error', errorCode: code, errorMessage: message }))
            // the free tier allows a few requests per minute: retry once automatically
            if (code === 'RATE_LIMIT' && !autoRetried.current.has(assistantId)) {
              autoRetried.current.add(assistantId)
              startCountdown(assistantId)
            }
          },
        },
        controller.signal,
      )
    },
    [scope, lang],
  )

  const startCountdown = (assistantId: string) => {
    let left = RETRY_SECONDS
    setRetryCountdown(left)
    retryTimer.current = window.setInterval(() => {
      left -= 1
      setRetryCountdown(left)
      if (left <= 0) {
        stopCountdown()
        retryRef.current(assistantId)
      }
    }, 1000)
  }

  const stopCountdown = () => {
    if (retryTimer.current) window.clearInterval(retryTimer.current)
    retryTimer.current = null
    setRetryCountdown(null)
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

  const retry = useCallback(
    (messageId: string) => {
      stopCountdown()
      const question = messagesRef.current.find((m) => m.id === messageId)?.question
      if (!question) return
      update(messageId, () => ({
        text: '',
        sources: undefined,
        status: 'searching',
        errorCode: undefined,
        errorMessage: undefined,
      }))
      run(messageId, question)
    },
    [run],
  )
  const retryRef = useRef(retry)
  retryRef.current = retry

  const newChat = useCallback(() => {
    abort.current?.abort()
    stopCountdown()
    del(`/chat/${conversationId.current}`).catch(() => {})
    conversationId.current = newId()
    autoRetried.current.clear()
    setMessages([])
  }, [])

  useEffect(() => () => stopCountdown(), [])

  const value = useMemo(
    () => ({ messages, scope, busy, retryCountdown, setScope, ask, retry, newChat }),
    [messages, scope, busy, retryCountdown, ask, retry, newChat],
  )
  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>
}

export function useChat() {
  const ctx = useContext(ChatContext)
  if (!ctx) throw new Error('useChat must be used inside ChatProvider')
  return ctx
}
