import {useEffect, useState} from 'react'
import {Link, useNavigate} from 'react-router-dom'
import {motion} from 'motion/react'
import {ChevronRight, ClipboardCheck, LoaderCircle, Radar, Trash} from 'lucide-react'
import {del, get, post} from '../lib/api'
import {useSettings} from '../lib/settings'
import {usePolling} from '../lib/usePolling'
import type {Law, LibraryDocument, Report} from '../lib/types'
import {PageHeader} from '../components/PageHeader'
import {ScoreRing} from '../components/ScoreRing'
import {StatusBadge} from '../components/StatusBadge'

export function CompliancePage() {
  const { t, lang } = useSettings()
  const navigate = useNavigate()
  const [laws, setLaws] = useState<Law[]>([])
  const [policies, setPolicies] = useState<LibraryDocument[]>([])
  const [policyId, setPolicyId] = useState('')
  const [lawId, setLawId] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { data: reports, setData: setReports } = usePolling(
    () => get<Report[]>('/compliance/reports'),
    (all) => all.some((r) => r.status === 'RUNNING'),
  )

  useEffect(() => {
    get<Law[]>('/compliance/laws').then(setLaws).catch(() => {})
    get<LibraryDocument[]>('/documents')
      .then((docs) => setPolicies(docs.filter((d) => d.mine && d.status === 'READY')))
      .catch(() => {})
  }, [])

  const start = async () => {
    setBusy(true)
    setError(null)
    try {
      const report = await post<Report>('/compliance/reports', { policyDocumentId: policyId, lawDocumentId: lawId, language: lang })
      navigate(`/compliance/${report.id}`)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const remove = async (id: string) => {
    await del(`/compliance/reports/${id}`)
    setReports((all) => all?.filter((r) => r.id !== id) ?? null)
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <PageHeader icon={<ClipboardCheck className="h-6 w-6" />} title={t('comp.title')} subtitle={t('comp.subtitle')} />

      <motion.div className="glass relative overflow-hidden p-6" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
        <div className="grid items-end gap-4 md:grid-cols-[1fr_auto_1fr_auto]">
          <label className="block">
            <span className="label-mono mb-1.5 block">{t('comp.policy')}</span>
            <select className="input" value={policyId} onChange={(e) => setPolicyId(e.target.value)}>
              <option value="">{t('comp.choosePolicy')}</option>
              {policies.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.title}
                </option>
              ))}
            </select>
          </label>
          <div className="hidden pb-3 font-display text-sm font-bold text-violet md:block">{t('comp.vs').toUpperCase()}</div>
          <label className="block">
            <span className="label-mono mb-1.5 block">{t('comp.law')}</span>
            <select className="input" value={lawId} onChange={(e) => setLawId(e.target.value)}>
              <option value="">{t('comp.chooseLaw')}</option>
              {laws.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.title} ({l.requirementCount})
                </option>
              ))}
            </select>
          </label>
          <button className="btn btn-neon py-3" disabled={!policyId || !lawId || busy} onClick={start}>
            {busy ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Radar className="h-4 w-4" />}
            {t('comp.run')}
          </button>
        </div>
        {policies.length === 0 && <p className="mt-4 text-sm text-amber">{t('comp.noPolicies')}</p>}
        {laws.length === 0 && <p className="mt-2 text-sm text-amber">{t('comp.noLaws')}</p>}
        {error && <p className="mt-3 text-sm text-red-300">{error}</p>}
      </motion.div>

      <h2 className="mb-4 mt-12 text-lg font-bold">{t('comp.reports')}</h2>
      {reports && reports.length === 0 && <div className="glass p-8 text-center text-muted">{t('comp.noReports')}</div>}
      <div className="space-y-3">
        {reports?.map((r, i) => (
          <motion.div key={r.id} initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.04 }}>
            <Link to={`/compliance/${r.id}`} className="glass glass-hover flex items-center gap-5 p-4">
              {r.status === 'DONE' && r.score !== null ? (
                <ScoreRing score={r.score} size={64} />
              ) : (
                <div className="grid h-16 w-16 place-items-center">
                  <StatusBadge status={r.status === 'RUNNING' ? 'RUNNING' : 'FAILED'} />
                </div>
              )}
              <div className="min-w-0 flex-1">
                <div className="truncate font-semibold">{r.policyTitle}</div>
                <div className="truncate text-sm text-muted">
                  {t('comp.vs')} {r.lawTitle}
                </div>
                {r.status === 'RUNNING' && (
                  <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-line">
                    <div className="scan-bar h-full rounded-full bg-gradient-to-r from-neon to-violet transition-all" style={{ width: `${r.total ? (r.processed / r.total) * 100 : 5}%` }} />
                  </div>
                )}
                {r.status === 'DONE' && (
                  <div className="mt-1 flex gap-3 text-xs">
                    <span className="text-acid">✓ {r.met}</span>
                    <span className="text-amber">◐ {r.partial}</span>
                    <span className="text-danger">✕ {r.missing}</span>
                  </div>
                )}
              </div>
              <span className="hidden text-xs text-muted sm:block">{new Date(r.createdAt).toLocaleString()}</span>
              <button
                onClick={(e) => {
                  e.preventDefault()
                  remove(r.id)
                }}
                className="rounded-lg p-2 text-muted hover:bg-danger/10 hover:text-danger"
                aria-label="delete"
              >
                <Trash className="h-4 w-4" />
              </button>
              <ChevronRight className="h-5 w-5 text-muted" />
            </Link>
          </motion.div>
        ))}
      </div>
    </div>
  )
}
