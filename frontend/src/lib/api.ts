import type {SearchScope, Source} from './types'

const TOKEN_KEY = 'cyberlaw.token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

/** An error from the backend: a stable code (translated in i18n.ts as "error.CODE"), its English message and params. */
export class ApiError extends Error {
  status: number
  code: string
  params: Record<string, unknown>

  constructor(status: number, code: string, message: string, params: Record<string, unknown> = {}) {
    super(message)
    this.status = status
    this.code = code
    this.params = params
  }
}

/** For responses without our JSON error body (e.g. a proxy error page). */
function codeForStatus(status: number) {
  if (status === 401) return 'UNAUTHORIZED'
  if (status === 403) return 'FORBIDDEN'
  if (status === 404) return 'NOT_FOUND'
  return status >= 500 ? 'INTERNAL' : 'BAD_REQUEST'
}

const networkError = (e: unknown) => new ApiError(0, 'NETWORK', (e as Error).message)

let onUnauthorized: () => void = () => {}

/** AuthProvider registers a callback that logs the user out when the token expires. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

async function toError(res: Response): Promise<ApiError> {
  let code = codeForStatus(res.status)
  let message = res.statusText || 'Request failed'
  let params: Record<string, unknown> = {}
  try {
    const body = await res.json()
    code = body.code ?? code
    message = body.message ?? message
    params = body.params ?? params
  } catch {
    /* not JSON */
  }
  if (res.status === 401) onUnauthorized()
  return new ApiError(res.status, code, message, params)
}

function authHeaders(): Record<string, string> {
  const token = tokenStore.get()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const isForm = options.body instanceof FormData
  let res: Response
  try {
    res = await fetch(`/api${path}`, {
      ...options,
      headers: {
        ...(isForm || !options.body ? {} : { 'Content-Type': 'application/json' }),
        ...authHeaders(),
        ...(options.headers ?? {}),
      },
    })
  } catch (e) {
    throw networkError(e)
  }
  if (!res.ok) throw await toError(res)
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export const get = <T>(path: string) => api<T>(path)
export const post = <T>(path: string, body?: unknown) =>
  api<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })
export const put = <T>(path: string, body: unknown) => api<T>(path, { method: 'PUT', body: JSON.stringify(body) })
export const del = (path: string) => api<void>(path, { method: 'DELETE' })

export function upload<T>(path: string, form: FormData) {
  return api<T>(path, { method: 'POST', body: form })
}

export interface StreamHandlers {
  onSources: (sources: Source[]) => void
  onToken: (text: string) => void
  onDone: () => void
  onError: (error: ApiError) => void
}

/**
 * POSTs a question and reads the Server-Sent Events stream by hand
 * (EventSource cannot send POST bodies or Authorization headers).
 */
export async function streamChat(
  body: { question: string; conversationId: string; scope: SearchScope; language: string },
  handlers: StreamHandlers,
  signal?: AbortSignal,
) {
  let res: Response
  try {
    res = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', ...authHeaders() },
      body: JSON.stringify(body),
      signal,
    })
  } catch (e) {
    if ((e as Error).name !== 'AbortError') handlers.onError(networkError(e))
    return
  }
  if (!res.ok || !res.body) {
    handlers.onError(await toError(res))
    return
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let finished = false

  const dispatch = (block: string) => {
    let event = 'message'
    const data: string[] = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
    }
    if (data.length === 0) return
    let payload: any
    try {
      payload = JSON.parse(data.join('\n'))
    } catch {
      return
    }
    switch (event) {
      case 'sources':
        handlers.onSources(payload as Source[])
        break
      case 'token':
        handlers.onToken(payload.text ?? '')
        break
      case 'done':
        finished = true
        handlers.onDone()
        break
      case 'error':
        finished = true
        handlers.onError(new ApiError(res.status, payload.code ?? 'AI_ERROR', payload.message ?? 'Error', payload.params))
        break
    }
  }

  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
      let sep: number
      while ((sep = buffer.indexOf('\n\n')) >= 0) {
        dispatch(buffer.slice(0, sep))
        buffer = buffer.slice(sep + 2)
      }
    }
    if (buffer.trim()) dispatch(buffer)
    if (!finished) handlers.onDone()
  } catch (e) {
    if ((e as Error).name !== 'AbortError') handlers.onError(networkError(e))
  }
}
