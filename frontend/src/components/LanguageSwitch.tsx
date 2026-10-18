import {useSettings} from '../lib/settings'
import type {Lang} from '../lib/i18n'

export function LanguageSwitch() {
  const { lang, setLang } = useSettings()
  return (
    <div className="inline-flex rounded-xl border border-line bg-panel/60 p-1 font-mono text-xs">
      {(['az', 'en'] as Lang[]).map((l) => (
        <button
          key={l}
          onClick={() => setLang(l)}
          className={`rounded-lg px-3 py-1.5 font-semibold uppercase transition-all ${
            lang === l ? 'bg-neon text-void shadow-[0_0_14px_-2px_var(--color-neon)]' : 'text-muted hover:text-white'
          }`}
        >
          {l}
        </button>
      ))}
    </div>
  )
}
