import {useEffect, useState} from 'react'
import {get} from './api'

/** Limits set in the backend's application.yml (GET /api/config), so the frontend has no copy of its own. */
export interface AppConfig {
  maxPrivateDocuments: number
  privateTtlDays: number
  maxFileSizeMb: number
  maxQuestionLength: number
}

let request: Promise<AppConfig> | null = null

/** Loaded once per page load; null until it has arrived. */
export function useAppConfig() {
  const [config, setConfig] = useState<AppConfig | null>(null)
  useEffect(() => {
    let active = true
    request ??= get<AppConfig>('/config')
    request
      .then((loaded) => active && setConfig(loaded))
      .catch(() => {
        request = null // try again next time
      })
    return () => {
      active = false
    }
  }, [])
  return config
}
