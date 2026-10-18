import {AnimatePresence} from 'motion/react'
import {FolderLock, Landmark, ListChecks, ShieldAlert} from 'lucide-react'
import {del, get} from '../lib/api'
import {useT} from '../lib/settings'
import {usePolling} from '../lib/usePolling'
import type {LibraryDocument} from '../lib/types'
import {PageHeader} from '../components/PageHeader'
import {UploadZone} from '../components/UploadZone'
import {DocumentCard} from '../components/DocumentCard'

export function DocumentsPage() {
  const t = useT()
  const { data, setData, poll } = usePolling(
    () => get<LibraryDocument[]>('/documents'),
    (docs) => docs.some((d) => d.status === 'PROCESSING'),
  )
  const docs = data ?? []
  const mine = docs.filter((d) => d.scope === 'PRIVATE')
  const official = docs.filter((d) => d.scope === 'OFFICIAL' && d.status === 'READY')

  const remove = async (id: string) => {
    await del(`/documents/${id}`)
    setData((all) => all?.filter((d) => d.id !== id) ?? null)
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <PageHeader icon={<FolderLock className="h-6 w-6" />} title={t('docs.title')} subtitle={t('docs.subtitle')} />

      <div className="grid gap-6 lg:grid-cols-[1fr_1.3fr]">
        <div className="space-y-4">
          <UploadZone
            scope="PRIVATE"
            onUploaded={(doc) => {
              setData((all) => [doc, ...(all ?? [])])
              poll() // keep refreshing until indexing is done
            }}
          />
          <div className="flex gap-3 rounded-xl border border-amber/30 bg-amber/5 p-3 text-xs text-amber-100/90">
            <ShieldAlert className="h-4 w-4 shrink-0 text-amber" />
            <div>
              <p>{t('docs.privacy')}</p>
              <p className="mt-1 text-muted">{t('docs.limits')}</p>
            </div>
          </div>
        </div>

        <div className="space-y-3">
          {data && mine.length === 0 && <div className="glass p-8 text-center text-muted">{t('docs.empty')}</div>}
          <AnimatePresence>
            {mine.map((d) => (
              <DocumentCard key={d.id} doc={d} onDelete={() => remove(d.id)} />
            ))}
          </AnimatePresence>
        </div>
      </div>

      <div className="mt-14">
        <div className="mb-4 flex items-center gap-3">
          <Landmark className="h-5 w-5 text-neon" />
          <div>
            <h2 className="text-lg font-bold">{t('docs.libraryTitle')}</h2>
            <p className="text-sm text-muted">{t('docs.librarySubtitle')}</p>
          </div>
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          {official.map((d) => (
            <DocumentCard key={d.id} doc={d}>
              {d.requirementsStatus === 'READY' && (
                <div className="mt-3 inline-flex items-center gap-1.5 text-xs text-acid">
                  <ListChecks className="h-3.5 w-3.5" /> {t('req.READY')}
                </div>
              )}
            </DocumentCard>
          ))}
          {data && official.length === 0 && <div className="glass p-6 text-center text-sm text-muted sm:col-span-2">{t('docs.empty')}</div>}
        </div>
      </div>
    </div>
  )
}
