import {useState} from 'react'
import {AnimatePresence, motion} from 'motion/react'
import {Landmark, ListChecks, LoaderCircle, WandSparkles} from 'lucide-react'
import {del, get, post} from '../lib/api'
import {useT} from '../lib/settings'
import {usePolling} from '../lib/usePolling'
import type {LibraryDocument, Requirement} from '../lib/types'
import {PageHeader} from '../components/PageHeader'
import {UploadZone} from '../components/UploadZone'
import {DocumentCard} from '../components/DocumentCard'
import {ErrorNote} from '../components/ErrorNote'

export function AdminPage() {
  const t = useT()
  const { data, setData, poll, error: loadError } = usePolling(
    () => get<LibraryDocument[]>('/documents').then((docs) => docs.filter((d) => d.scope === 'OFFICIAL')),
    (docs) => docs.some((d) => d.status === 'PROCESSING' || d.requirementsStatus === 'EXTRACTING'),
  )
  const [error, setError] = useState<unknown>(null)

  const replace = (doc: LibraryDocument) => setData((all) => all?.map((d) => (d.id === doc.id ? doc : d)) ?? null)

  const generate = async (id: string) => {
    setError(null)
    try {
      replace(await post<LibraryDocument>(`/admin/documents/${id}/requirements`))
      poll()
    } catch (e) {
      setError(e)
    }
  }

  const remove = async (id: string) => {
    setError(null)
    try {
      await del(`/documents/${id}`)
      setData((all) => all?.filter((d) => d.id !== id) ?? null)
    } catch (e) {
      setError(e)
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <PageHeader icon={<Landmark className="h-6 w-6" />} title={t('admin.title')} subtitle={t('admin.subtitle')} />

      <UploadZone
        scope="OFFICIAL"
        onUploaded={(doc) => {
          setData((all) => [doc, ...(all ?? [])])
          poll()
        }}
      />
      <ErrorNote error={error ?? loadError} className="mt-4" />

      <div className="mt-8 grid gap-3">
        <AnimatePresence>
          {data?.map((d) => (
            <DocumentCard key={d.id} doc={d} onDelete={() => remove(d.id)}>
              {d.status === 'READY' && <ChecklistControls doc={d} onGenerate={() => generate(d.id)} />}
            </DocumentCard>
          ))}
        </AnimatePresence>
        {data && data.length === 0 && <div className="glass p-8 text-center text-muted">{t('docs.empty')}</div>}
      </div>
    </div>
  )
}

function ChecklistControls({ doc, onGenerate }: { doc: LibraryDocument; onGenerate: () => void }) {
  const t = useT()
  const [items, setItems] = useState<Requirement[] | null>(null)
  const [error, setError] = useState<unknown>(null)
  const extracting = doc.requirementsStatus === 'EXTRACTING'

  const toggle = async () => {
    setError(null)
    if (items) {
      setItems(null)
      return
    }
    try {
      setItems(await get<Requirement[]>(`/compliance/laws/${doc.id}/requirements`))
    } catch (e) {
      setError(e)
    }
  }

  return (
    <div className="mt-4 border-t border-line pt-3">
      <div className="flex flex-wrap items-center gap-2">
        <span className={`chip ${doc.requirementsStatus === 'READY' ? 'text-acid' : doc.requirementsStatus === 'FAILED' ? 'text-danger' : 'text-muted'}`}>
          {extracting ? <LoaderCircle className="h-3 w-3 animate-spin" /> : <ListChecks className="h-3 w-3" />}
          {t(`req.${doc.requirementsStatus}`)}
        </span>
        <div className="flex-1" />
        {doc.requirementsStatus === 'READY' && (
          <button className="btn btn-ghost py-1.5 text-xs" onClick={toggle}>
            {items ? t('admin.hideChecklist') : t('admin.viewChecklist')}
          </button>
        )}
        <button className="btn btn-ghost py-1.5 text-xs" disabled={extracting} onClick={onGenerate}>
          <WandSparkles className="h-3.5 w-3.5" />
          {doc.requirementsStatus === 'READY' ? t('admin.regenerate') : t('admin.generate')}
        </button>
      </div>
      <ErrorNote error={error} className="mt-3" />
      <AnimatePresence>
        {items && (
          <motion.ol
            className="mt-3 space-y-2 overflow-hidden text-sm"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
          >
            {items.map((r) => (
              <li key={r.position} className="rounded-lg border border-line bg-void/40 p-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-mono text-xs text-neon">R{r.position}</span>
                  <span className="font-semibold">{r.title}</span>
                  {r.article && <span className="chip text-violet">{r.article}</span>}
                </div>
                <p className="mt-1 text-slate-400">{r.description}</p>
              </li>
            ))}
          </motion.ol>
        )}
      </AnimatePresence>
    </div>
  )
}
