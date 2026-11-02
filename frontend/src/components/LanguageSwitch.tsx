import {useSettings} from '../lib/settings'
import type {Lang} from '../lib/i18n'

export function LanguageSwitch() {
  const { lang, setLang } = useSettings()
  return (
    <div className="inline-flex rounded-md border border-line bg-panel/60 p-1 font-mono text-xs">
      {(['az', 'en'] as Lang[]).map((l) => (
        <button
          key={l}
          onClick={() => setLang(l)}
          className={`rounded-sm px-3 py-1.5 font-semibold uppercase transition-all ${
            lang === l ? 'bg-neon text-void' : 'text-muted hover:text-ink'
          }`}
        >
          {l}
        </button>
      ))}
    </div>
  )
}
