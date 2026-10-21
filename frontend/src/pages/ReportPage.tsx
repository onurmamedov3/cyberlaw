import {useState} from 'react'
import {Link, useParams} from 'react-router-dom'
import {AnimatePresence, motion} from 'motion/react'
import {ArrowLeft, BookOpen, ChevronDown, CircleCheck, CircleX, Printer, Quote, TriangleAlert} from 'lucide-react'
import {get} from '../lib/api'
import {useErrorText} from '../lib/errors'
import {useT} from '../lib/settings'
import {usePolling} from '../lib/usePolling'
import type {ReportDetail, ReportResult, Verdict} from '../lib/types'
import {ScoreRing} from '../components/ScoreRing'

const VERDICT = {
  MET: { icon: CircleCheck, color: 'text-acid', border: 'border-acid/40', bg: 'bg-acid/10' },
  PARTIAL: { icon: TriangleAlert, color: 'text-amber', border: 'border-amber/40', bg: 'bg-amber/10' },
  MISSING: { icon: CircleX, color: 'text-danger', border: 'border-danger/40', bg: 'bg-danger/10' },
} as const

export function ReportPage() {
  const t = useT()
  const errorText = useErrorText()
  const { id } = useParams()
  const [filter, setFilter] = useState<Verdict | 'ALL'>('ALL')
  const { data, error } = usePolling(
    () => get<ReportDetail>(`/compliance/reports/${id}`),
    (d) => d.report.status === 'RUNNING',
    2500,
  )

  if (error) return <div className="p-10 text-center text-red-300">{errorText(error)}</div>
  if (!data) return <div className="p-10 text-center text-muted">{t('common.loading')}</div>

  const { report, results } = data
  const shown = filter === 'ALL' ? results : results.filter((r) => r.status === filter)
  const progress = report.total ? Math.round((report.processed / report.total) * 100) : 0

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3 print:hidden">
        <Link to="/compliance" className="inline-flex items-center gap-2 text-sm text-muted hover:text-neon">
          <ArrowLeft className="h-4 w-4" /> {t('comp.back')}
        </Link>
        {report.status === 'DONE' && (
          // the browser's print dialog also offers "Save as PDF"
          <button className="btn btn-ghost py-2" onClick={() => window.print()}>
            <Printer className="h-4 w-4" /> {t('comp.print')}
          </button>
        )}
      </div>

      <motion.div className="glass flex flex-col items-center gap-8 p-8 md:flex-row" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
        {report.status === 'DONE' && report.score !== null ? (
          <ScoreRing score={report.score} size={170} label={t('comp.score')} />
        ) : (
          <div className="grid h-[170px] w-[170px] place-items-center rounded-full border-4 border-dashed border-neon/30">
            <div className="text-center">
              <div className="font-display text-3xl font-extrabold text-neon">{progress}%</div>
              <div className="label-mono mt-1 !text-[0.6rem]">{t('comp.progress')}</div>
            </div>
          </div>
        )}
        <div className="min-w-0 flex-1 text-center md:text-left">
          <div className="label-mono">{t('comp.policy')}</div>
          <h1 className="text-xl font-bold">{report.policyTitle}</h1>
          <div className="label-mono mt-3">{t('comp.law')}</div>
          <h2 className="text-lg font-semibold text-neon-soft">{report.lawTitle}</h2>
          {report.status === 'FAILED' && (
            <p className="mt-3 text-sm text-red-300">{errorText({ code: report.errorCode, message: report.errorMessage })}</p>
          )}
          <div className="mt-5 grid grid-cols-3 gap-3">
            {(['MET', 'PARTIAL', 'MISSING'] as const).map((v) => {
              const count = v === 'MET' ? report.met : v === 'PARTIAL' ? report.partial : report.missing
              return (
                <div key={v} className={`rounded-xl border ${VERDICT[v].border} ${VERDICT[v].bg} p-3 text-center`}>
                  <div className={`font-display text-2xl font-extrabold ${VERDICT[v].color}`}>{count}</div>
                  <div className="text-xs text-muted">{t(`comp.${v}`)}</div>
                </div>
              )
            })}
          </div>
        </div>
      </motion.div>

      {report.status === 'RUNNING' && (
        <div className="mt-6 h-2 overflow-hidden rounded-full bg-line">
          <div className="scan-bar h-full rounded-full bg-gradient-to-r from-neon to-violet transition-all duration-700" style={{ width: `${Math.max(progress, 4)}%` }} />
        </div>
      )}

      <div className="mt-8 flex flex-wrap gap-2 print:hidden">
        {(['ALL', 'MISSING', 'PARTIAL', 'MET'] as const).map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
              filter === f ? 'bg-neon/15 text-neon shadow-[inset_0_0_0_1px_rgb(34_211_238/0.5)]' : 'text-muted hover:text-white'
            }`}
          >
            {f === 'ALL' ? t('comp.all') : t(`comp.${f}`)}
          </button>
        ))}
      </div>

      <div className="mt-4 space-y-3">
        {shown.map((r) => (
          <ResultRow key={r.position} result={r} />
        ))}
      </div>
    </div>
  )
}

function ResultRow({ result }: { result: ReportResult }) {
  const t = useT()
  const [open, setOpen] = useState(result.status !== 'MET')
  const style = VERDICT[result.status]
  const Icon = style.icon
  const details = (
    <div className="space-y-3 border-t border-line px-4 pb-4 pt-3 pl-12 text-sm">
      <div>
        <div className="label-mono mb-1">{t('comp.requirement')}</div>
        <p className="text-slate-300">{result.description}</p>
      </div>
      {result.evidence && (
        <div>
          <div className="label-mono mb-1">{t('comp.evidence')}</div>
          <blockquote className="flex gap-2 rounded-lg border border-violet/30 bg-violet/10 p-3 italic text-slate-200">
            <Quote className="h-4 w-4 shrink-0 text-violet" />
            {result.evidence}
          </blockquote>
        </div>
      )}
    </div>
  )
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      className={`glass border-l-4 break-inside-avoid ${style.border}`}
    >
      <button className="flex w-full items-start gap-3 p-4 text-left" onClick={() => setOpen(!open)}>
        <Icon className={`mt-0.5 h-5 w-5 shrink-0 ${style.color}`} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold">{result.title}</span>
            {result.article && (
              <span className="chip text-neon">
                <BookOpen className="h-3 w-3" /> {result.article}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-slate-300">{result.explanation}</p>
        </div>
        <ChevronDown className={`h-4 w-4 shrink-0 text-muted transition-transform print:hidden ${open ? 'rotate-180' : ''}`} />
      </button>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
            {details}
          </motion.div>
        )}
      </AnimatePresence>
      {/* a printed report shows every requirement in full */}
      {!open && <div className="hidden print:block">{details}</div>}
    </motion.div>
  )
}
