import {type ReactNode, useEffect, useState} from 'react'
import {motion} from 'motion/react'
import {BrainCircuit, CircleCheck, CircleUser, History, Languages, Settings, Trash, TriangleAlert, Zap} from 'lucide-react'
import {get} from '../lib/api'
import {useAuth} from '../lib/auth'
import {useChat} from '../lib/chat'
import {useAppConfig} from '../lib/config'
import {useSettings} from '../lib/settings'
import type {Lang} from '../lib/i18n'
import {useConfirm} from '../components/ConfirmDialog'
import {ErrorNote} from '../components/ErrorNote'
import {PageHeader} from '../components/PageHeader'

export function SettingsPage() {
  const { t, lang, setLang, animations, setAnimations } = useSettings()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [aiConfigured, setAiConfigured] = useState<boolean | null>(null)

  useEffect(() => {
    if (!isAdmin) return
    get<{ aiConfigured: boolean }>('/health')
      .then((h) => setAiConfigured(h.aiConfigured))
      .catch(() => setAiConfigured(false))
  }, [isAdmin])

  const languages: { code: Lang; name: string; flag: string }[] = [
    { code: 'az', name: 'Azərbaycan dili', flag: '🇦🇿' },
    { code: 'en', name: 'English', flag: '🇬🇧' },
  ]

  return (
    <div className="workspace-page workspace-page-narrow">
      <PageHeader icon={<Settings className="h-6 w-6" />} title={t('settings.title')} />

      <div className="space-y-4">
        <Section inline icon={<Languages className="h-5 w-5" />} title={t('settings.language')} hint={t('settings.languageHint')}>
          <div className="grid w-full gap-2 sm:grid-cols-2 lg:w-auto">
            {languages.map((l) => (
              <button
                key={l.code}
                onClick={() => setLang(l.code)}
                aria-pressed={lang === l.code}
                className={`flex items-center gap-2 rounded-md border px-3 py-2.5 text-left text-sm transition-colors ${
                  lang === l.code
                    ? 'border-neon bg-neon/10'
                    : 'border-line hover:border-neon/40'
                }`}
              >
                <span className="text-lg" aria-hidden>{l.flag}</span>
                <span className="whitespace-nowrap font-semibold">{l.name}</span>
                <CircleCheck aria-hidden className={`ml-auto h-4 w-4 shrink-0 text-neon ${lang === l.code ? '' : 'invisible'}`} />
              </button>
            ))}
          </div>
        </Section>

        <Section inline icon={<Zap className="h-5 w-5" />} title={t('settings.animations')} hint={t('settings.animationsHint')}>
          <button
            onClick={() => setAnimations(!animations)}
            className={`relative h-8 w-16 rounded-full border transition-all ${animations ? 'border-neon bg-neon/20' : 'border-line bg-panel'}`}
            aria-label={t('settings.animations')}
            aria-pressed={animations}
          >
            <motion.span
              className={`absolute top-1 h-5.5 w-5.5 rounded-full ${animations ? 'bg-neon' : 'bg-muted'}`}
              animate={{ left: animations ? 36 : 4 }}
              transition={{ type: 'spring', stiffness: 400, damping: 28 }}
            />
          </button>
          <span className="text-sm text-muted">{animations ? t('settings.on') : t('settings.off')}</span>
        </Section>

        {isAdmin && <Section icon={<BrainCircuit className="h-5 w-5" />} title={t('settings.aiStatus')}>
          {aiConfigured === null ? (
            <span className="text-sm text-muted">{t('common.loading')}</span>
          ) : aiConfigured ? (
            <span className="chip text-acid">
              <CircleCheck className="h-3 w-3" /> {t('settings.aiOk')}
            </span>
          ) : (
            <span className="chip text-amber">
              <TriangleAlert className="h-3 w-3" />{' '}
              {t('settings.aiMissingAdmin')}
            </span>
          )}
        </Section>}

        <ChatHistorySection />

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

/** How long chats are kept, and "Delete all chats". */
function ChatHistorySection() {
  const { t } = useSettings()
  const { conversations, deleteAllConversations } = useChat()
  const config = useAppConfig()
  const confirm = useConfirm()
  const [state, setState] = useState<'idle' | 'busy' | 'done'>('idle')
  const [error, setError] = useState<unknown>(null)

  const clear = async () => {
    if (!(await confirm({ message: t('settings.confirmClear'), confirmLabel: t('settings.clearHistory') }))) return
    setState('busy')
    setError(null)
    try {
      await deleteAllConversations()
      setState('done')
    } catch (e) {
      setError(e)
      setState('idle')
    }
  }

  const hint = !config
    ? undefined
    : config.historyDays > 0
      ? t('settings.historyDays', { days: config.historyDays })
      : t('settings.historyKept')

  return (
    <Section icon={<History className="h-5 w-5" />} title={t('settings.history')} hint={hint}>
      <button
        className="btn btn-danger"
        onClick={clear}
        disabled={state === 'busy' || conversations?.length === 0}
      >
        <Trash className="h-4 w-4" /> {t('settings.clearHistory')}
      </button>
      {state === 'done' && <span className="ml-3 text-sm text-acid">{t('settings.cleared')}</span>}
      <ErrorNote error={error} className="mt-3 w-full" />
    </Section>
  )
}

function Section({ icon, title, hint, children, inline = false }: { icon: ReactNode; title: string; hint?: string; children: ReactNode; inline?: boolean }) {
  return (
    <motion.section className={`glass p-5 ${inline ? 'flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between' : ''}`} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
      <div className={`flex min-w-0 items-center gap-3 ${inline ? 'lg:flex-1' : 'mb-4'}`}>
        <span className="shrink-0 text-neon">{icon}</span>
        <div className="min-w-0">
          <h2 className="font-semibold">{title}</h2>
          {hint && <p className="text-xs text-muted">{hint}</p>}
        </div>
      </div>
      <div className={`flex flex-wrap items-center ${inline ? 'w-full gap-3 lg:w-auto lg:shrink-0' : ''}`}>{children}</div>
    </motion.section>
  )
}
