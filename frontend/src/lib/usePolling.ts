import {useCallback, useEffect, useRef, useState} from 'react'

/**
 * Loads data once, then keeps re-loading every `intervalMs` while `shouldPoll(data)` is true
 * (e.g. while a document is still being indexed).
 */
export function usePolling<T>(load: () => Promise<T>, shouldPoll: (data: T) => boolean, intervalMs = 3000) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const loadRef = useRef(load)
  loadRef.current = load
  const pollRef = useRef(shouldPoll)
  pollRef.current = shouldPoll

  const refresh = useCallback(async () => {
    try {
      const result = await loadRef.current()
      setData(result)
      setError(null)
      return result
    } catch (e) {
      setError((e as Error).message)
      return null
    }
  }, [])

  useEffect(() => {
    let timer: number | undefined
    let cancelled = false
    const tick = async () => {
      const result = await refresh()
      if (!cancelled && result !== null && pollRef.current(result)) {
        timer = window.setTimeout(tick, intervalMs)
      }
    }
    tick()
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [refresh, intervalMs, version])

  /** Restart polling, e.g. right after an upload started a new background job. */
  const poll = useCallback(() => setVersion((v) => v + 1), [])

  return { data, setData, error, refresh, poll }
}
