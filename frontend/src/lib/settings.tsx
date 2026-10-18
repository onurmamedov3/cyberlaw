import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState} from 'react'
import {dictionaries, type Lang, type TKey} from './i18n'

interface Settings {
  lang: Lang
  animations: boolean
}

interface SettingsContextValue extends Settings {
  setLang: (lang: Lang) => void
  setAnimations: (on: boolean) => void
  t: (key: TKey) => string
}

const STORAGE_KEY = 'cyberlaw.settings'

function load(): Settings {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}')
    return { lang: saved.lang === 'en' ? 'en' : 'az', animations: saved.animations !== false }
  } catch {
    return { lang: 'az', animations: true }
  }
}

const SettingsContext = createContext<SettingsContextValue | null>(null)

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<Settings>(load)

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
    document.documentElement.lang = settings.lang
    document.documentElement.classList.toggle('no-anim', !settings.animations)
  }, [settings])

  const t = useCallback((key: TKey) => dictionaries[settings.lang][key] ?? key, [settings.lang])

  const value = useMemo<SettingsContextValue>(
    () => ({
      ...settings,
      t,
      setLang: (lang) => setSettings((s) => ({ ...s, lang })),
      setAnimations: (animations) => setSettings((s) => ({ ...s, animations })),
    }),
    [settings, t],
  )

  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>
}

export function useSettings() {
  const ctx = useContext(SettingsContext)
  if (!ctx) throw new Error('useSettings must be used inside SettingsProvider')
  return ctx
}

/** Shortcut for the translate function. */
export function useT() {
  return useSettings().t
}
