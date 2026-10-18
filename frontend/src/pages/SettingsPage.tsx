import {type ReactNode, useEffect, useState} from 'react'
import {motion} from 'motion/react'
import {BrainCircuit, CircleCheck, CircleUser, Languages, Settings, TriangleAlert, Zap} from 'lucide-react'
import {get} from '../lib/api'
import {useAuth} from '../lib/auth'
import {useSettings} from '../lib/settings'
import type {Lang} from '../lib/i18n'
import {PageHeader} from '../components/PageHeader'

export function SettingsPage() {
  const { t, lang, setLang, animations, setAnimations } = useSettings()
  const { user } = useAuth()
  const [aiConfigured, setAiConfigured] = useState<boolean | null>(null)

  useEffect(() => {
    get<{ aiConfigured: boolean }>('/health')
      .then((h) => setAiConfigured(h.aiConfigured))
      .catch(() => setAiConfigured(false))
  }, [])

  const languages: { code: Lang; name: string; flag: string }[] = [
    { code: 'az', name: 'Azərbaycan dili', flag: '🇦🇿' },
    { code: 'en', name: 'English', flag: '🇬🇧' },
  ]

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <PageHeader icon={<Settings className="h-6 w-6" />} title={t('settings.title')} />

      <div className="space-y-4">
        <Section icon={<Languages className="h-5 w-5" />} title={t('settings.language')} hint={t('settings.languageHint')}>
          <div className="grid gap-3 sm:grid-cols-2">
            {languages.map((l) => (
              <button
                key={l.code}
                onClick={() => setLang(l.code)}
                className={`flex items-center gap-3 rounded-xl border p-4 text-left transition-all ${
                  lang === l.code
                    ? 'border-neon bg-neon/10 shadow-[0_0_24px_-8px_var(--color-neon)]'
                    : 'border-line hover:border-neon/40'
                }`}
              >
                <span className="text-2xl">{l.flag}</span>
                <span className="font-semibold">{l.name}</span>
                {lang === l.code && <CircleCheck className="ml-auto h-5 w-5 text-neon" />}
              </button>
            ))}
          </div>
        </Section>

        <Section icon={<Zap className="h-5 w-5" />} title={t('settings.animations')} hint={t('settings.animationsHint')}>
          <button
            onClick={() => setAnimations(!animations)}
            className={`relative h-8 w-16 rounded-full border transition-all ${animations ? 'border-neon bg-neon/20' : 'border-line bg-panel'}`}
            aria-pressed={animations}
          >
            <motion.span
              className={`absolute top-1 h-5.5 w-5.5 rounded-full ${animations ? 'bg-neon shadow-[0_0_12px_var(--color-neon)]' : 'bg-muted'}`}
              animate={{ left: animations ? 36 : 4 }}
              transition={{ type: 'spring', stiffness: 400, damping: 28 }}
            />
          </button>
          <span className="ml-3 text-sm text-muted">{animations ? t('settings.on') : t('settings.off')}</span>
        </Section>

        <Section icon={<BrainCircuit className="h-5 w-5" />} title={t('settings.aiStatus')}>
          {aiConfigured === null ? (
            <span className="text-sm text-muted">{t('common.loading')}</span>
          ) : aiConfigured ? (
            <span className="chip text-acid">
              <CircleCheck className="h-3 w-3" /> {t('settings.aiOk')}
            </span>
          ) : (
            <span className="chip text-amber">
              <TriangleAlert className="h-3 w-3" /> {t('settings.aiMissing')}
            </span>
          )}
        </Section>

        <Section icon={<CircleUser className="h-5 w-5" />} title={t('settings.account')}>
          <div className="text-sm">
            <div className="font-semibold">{user?.displayName}</div>
            <div className="text-muted">{user?.email}</div>
            <span className="chip mt-2 text-violet">{user?.role}</span>
          </div>
        </Section>
      </div>
    </div>
  )
}

function Section({ icon, title, hint, children }: { icon: ReactNode; title: string; hint?: string; children: ReactNode }) {
  return (
    <motion.section className="glass p-5" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
      <div className="mb-4 flex items-center gap-3">
        <span className="text-neon">{icon}</span>
        <div>
          <h2 className="font-semibold">{title}</h2>
          {hint && <p className="text-xs text-muted">{hint}</p>}
        </div>
      </div>
      <div className="flex flex-wrap items-center">{children}</div>
    </motion.section>
  )
}
