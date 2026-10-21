import {type FormEvent, type ReactNode, useState} from 'react'
import {motion} from 'motion/react'
import {BookOpen, FolderLock, KeyRound, LoaderCircle, Mail, Radar, User} from 'lucide-react'
import {useAuth} from '../lib/auth'
import {useT} from '../lib/settings'
import {ErrorNote} from '../components/ErrorNote'
import {Logo} from '../components/Logo'
import {LanguageSwitch} from '../components/LanguageSwitch'

export function LoginPage() {
  const t = useT()
  const { login, register } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      if (mode === 'login') await login(email, password)
      else await register(email, password, name)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  const features = [
    { icon: BookOpen, text: t('auth.feature1') },
    { icon: FolderLock, text: t('auth.feature2') },
    { icon: Radar, text: t('auth.feature3') },
  ]

  return (
    <div className="flex min-h-full items-center justify-center p-6">
      <div className="absolute right-6 top-6">
        <LanguageSwitch />
      </div>

      <div className="grid w-full max-w-5xl items-center gap-10 lg:grid-cols-2">
        <motion.div initial={{ opacity: 0, x: -30 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.6 }}>
          <Logo size="lg" />
          <p className="mt-5 text-lg text-slate-300">{t('brand.tagline')}</p>
          <ul className="mt-8 space-y-4">
            {features.map(({ icon: Icon, text }, i) => (
              <motion.li
                key={text}
                className="flex items-center gap-3 text-slate-300"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 + i * 0.15 }}
              >
                <span className="grid h-9 w-9 place-items-center rounded-lg border border-neon/30 bg-neon/10">
                  <Icon className="h-4.5 w-4.5 text-neon" />
                </span>
                {text}
              </motion.li>
            ))}
          </ul>
          <div className="label-mono mt-10 hidden lg:block">
            &gt; secure_channel: <span className="text-acid">established</span>
            <span className="typing-cursor" />
          </div>
        </motion.div>

        <motion.form
          onSubmit={submit}
          className="glass p-8 shadow-[0_0_60px_-20px_var(--color-neon)]"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
        >
          <h2 className="text-2xl font-bold">{mode === 'login' ? t('auth.login') : t('auth.register')}</h2>
          <p className="mt-1 text-sm text-muted">{t('auth.welcome')}</p>

          <div className="mt-7 space-y-4">
            {mode === 'register' && (
              <Field icon={User} label={t('auth.name')}>
                <input className="input pl-10" value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />
              </Field>
            )}
            <Field icon={Mail} label={t('auth.email')}>
              <input className="input pl-10" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </Field>
            <Field icon={KeyRound} label={t('auth.password')}>
              <input
                className="input pl-10"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={mode === 'register' ? 8 : undefined}
                placeholder={mode === 'register' ? t('auth.passwordHint') : undefined}
              />
            </Field>
          </div>

          <ErrorNote error={error} className="mt-4" />

          <button className="btn btn-neon mt-6 w-full py-3" disabled={busy}>
            {busy && <LoaderCircle className="h-4 w-4 animate-spin" />}
            {mode === 'login' ? t('auth.login') : t('auth.register')}
          </button>

          <p className="mt-5 text-center text-sm text-muted">
            {mode === 'login' ? t('auth.noAccount') : t('auth.haveAccount')}{' '}
            <button
              type="button"
              className="font-semibold text-neon hover:underline"
              onClick={() => {
                setMode(mode === 'login' ? 'register' : 'login')
                setError(null)
              }}
            >
              {mode === 'login' ? t('auth.register') : t('auth.login')}
            </button>
          </p>
        </motion.form>
      </div>
    </div>
  )
}

function Field({ icon: Icon, label, children }: { icon: typeof Mail; label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="label-mono mb-1.5 block">{label}</span>
      <span className="relative block">
        <Icon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
        {children}
      </span>
    </label>
  )
}
