import {useCallback} from 'react'
import {ApiError} from './api'
import {dictionaries, format} from './i18n'
import {useSettings} from './settings'

/** An error as the backend stores or sends it, e.g. a failed document's errorCode + errorMessage. */
export interface ErrorParts {
  code?: string | null
  message?: string | null
  params?: Record<string, unknown>
}

function partsOf(error: unknown): ErrorParts {
  if (error instanceof ApiError) return { code: error.code, message: error.message, params: error.params }
  if (typeof error === 'string') return { message: error }
  // a plain Error is a bug in the page, whose message means nothing to the user
  if (error instanceof Error || !error || typeof error !== 'object') return {}
  return error as ErrorParts
}

/**
 * The text to show for an error: our translation of its code ("error.CODE" in i18n.ts), else the
 * server's English message, else a generic "Something went wrong".
 */
export function useErrorText() {
  const { lang, t } = useSettings()
  return useCallback(
    (error: unknown) => {
      const { code, message, params } = partsOf(error)
      const translated = code ? (dictionaries[lang] as Record<string, string>)[`error.${code}`] : undefined
      if (translated) return format(translated, params)
      return message || t('common.error')
    },
    [lang, t],
  )
}
