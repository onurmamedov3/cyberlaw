import {type ReactNode, useState} from 'react'
import {AnimatePresence} from 'motion/react'
import {FileUp, FolderLock, Landmark, ListChecks, ShieldAlert} from 'lucide-react'
import {del, get} from '../lib/api'
import {useT} from '../lib/settings'
import {useAppConfig} from '../lib/config'
import {usePolling} from '../lib/usePolling'
import type {LibraryDocument} from '../lib/types'
import {PageHeader} from '../components/PageHeader'
import {UploadZone} from '../components/UploadZone'
import {DocumentCard} from '../components/DocumentCard'
import {ErrorNote} from '../components/ErrorNote'

export function DocumentsPage() {
  const t = useT()
  const { data, setData, poll, error } = usePolling(
    () => get<LibraryDocument[]>('/documents'),
    (docs) => docs.some((d) => d.status === 'PROCESSING'),
  )
  const [deleteError, setDeleteError] = useState<unknown>(null)
  const config = useAppConfig()
  const docs = data ?? []
  const mine = docs.filter((d) => d.scope === 'PRIVATE')
  const official = docs.filter((d) => d.scope === 'OFFICIAL' && d.status === 'READY')

  const remove = async (id: string) => {
    setDeleteError(null)
    try {
      await del(`/documents/${id}`)
      setData((all) => all?.filter((d) => d.id !== id) ?? null)
    } catch (e) {
      setDeleteError(e)
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <PageHeader icon={<FolderLock className="h-6 w-6" />} title={t('docs.title')} subtitle={t('docs.subtitle')} />

      <UploadZone
        scope="PRIVATE"
        onUploaded={(doc) => {
          setData((all) => [doc, ...(all ?? [])])
          poll() // keep refreshing until indexing is done
        }}
      />
      <div className="mt-4 flex gap-3 rounded-xl border border-amber/30 bg-amber/5 p-3 text-xs text-amber-100/90">
        <ShieldAlert className="h-4 w-4 shrink-0 text-amber" />
        <div>
          <p>{t('docs.privacy')}</p>
          {config && (
            <p className="mt-1 text-muted">
              {t('docs.limits', { max: config.maxPrivateDocuments, days: config.privateTtlDays })}
            </p>
          )}
        </div>
      </div>
      <ErrorNote error={deleteError ?? error} className="mt-4" />

      <Section
        icon={<FolderLock className="h-5 w-5" />}
        title={t('docs.uploaded')}
        aside={config && <QuotaMeter used={mine.length} max={config.maxPrivateDocuments} />}
      >
        {data && mine.length === 0 && (
          <div className="glass flex flex-col items-center gap-2 p-8 text-center text-muted">
            <FileUp className="h-8 w-8 text-neon/60" />
            <p className="font-medium text-slate-300">{t('docs.empty')}</p>
            <p className="text-sm">{t('docs.emptyHint')}</p>
          </div>
        )}
        <AnimatePresence>
          {mine.map((d) => (
            <DocumentCard key={d.id} doc={d} onDelete={() => remove(d.id)} />
          ))}
        </AnimatePresence>
      </Section>

      <Section
        icon={<Landmark className="h-5 w-5" />}
        title={t('docs.libraryTitle')}
        subtitle={t('docs.librarySubtitle')}
        aside={data && <span className="chip text-neon">{official.length}</span>}
      >
        {data && official.length === 0 && <div className="glass p-6 text-center text-sm text-muted">{t('docs.empty')}</div>}
        {official.map((d) => (
          <DocumentCard key={d.id} doc={d}>
            {d.requirementsStatus === 'READY' && (
              <div className="mt-3 inline-flex items-center gap-1.5 text-xs text-acid">
                <ListChecks className="h-3.5 w-3.5" /> {t('req.READY')}
              </div>
            )}
          </DocumentCard>
        ))}
      </Section>
    </div>
  )
}

/** A titled block with its cards stacked vertically, like the Admin library list. */
function Section({
  icon,
  title,
  subtitle,
  aside,
  children,
}: {
  icon: ReactNode
  title: string
  subtitle?: string
  aside?: ReactNode
  children: ReactNode
}) {
  return (
    <section className="mt-10">
      <div className="mb-4 flex items-center gap-3 border-b border-line pb-3">
        <span className="text-neon">{icon}</span>
        <div className="min-w-0 flex-1">
          <h2 className="text-lg font-bold">{title}</h2>
          {subtitle && <p className="text-sm text-muted">{subtitle}</p>}
        </div>
        {aside}
      </div>
      <div className="grid gap-3">{children}</div>
    </section>
  )
}

/** "2 / 5" with a small bar that turns amber when the upload limit is reached. */
function QuotaMeter({ used, max }: { used: number; max: number }) {
  const full = used >= max
  return (
    <div className="flex items-center gap-3">
      <div className="hidden h-1.5 w-24 overflow-hidden rounded-full bg-line sm:block">
        <div
          className={`h-full rounded-full transition-all ${full ? 'bg-amber' : 'bg-gradient-to-r from-neon to-violet'}`}
          style={{ width: `${Math.min(100, (used / max) * 100)}%` }}
        />
      </div>
      <span className={`font-mono text-xs font-semibold ${full ? 'text-amber' : 'text-muted'}`}>
        {used} / {max}
      </span>
    </div>
  )
}
