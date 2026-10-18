import type {ReactNode} from 'react'
import {motion} from 'motion/react'
import {Clock, FileText, Trash} from 'lucide-react'
import {useT} from '../lib/settings'
import type {LibraryDocument} from '../lib/types'
import {StatusBadge} from './StatusBadge'

function daysLeft(expiresAt: string) {
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - Date.now()) / 86_400_000))
}

export function DocumentCard({
  doc,
  onDelete,
  children,
}: {
  doc: LibraryDocument
  onDelete?: () => void
  children?: ReactNode
}) {
  const t = useT()
  return (
    <motion.div layout initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, scale: 0.95 }} className="glass glass-hover p-4">
      <div className="flex items-start gap-3">
        <div className={`grid h-10 w-10 shrink-0 place-items-center rounded-lg border ${doc.status === 'PROCESSING' ? 'scan-bar border-amber/40 bg-amber/10' : 'border-neon/30 bg-neon/10'}`}>
          <FileText className={`h-5 w-5 ${doc.status === 'PROCESSING' ? 'text-amber' : 'text-neon'}`} />
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate font-semibold" title={doc.title}>
            {doc.title}
          </div>
          <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-muted">
            <StatusBadge status={doc.status} />
            {doc.language && <span className="chip text-violet uppercase">{doc.language}</span>}
            {doc.status === 'READY' && (
              <span>
                {doc.chunkCount} {t('docs.chunks')}
              </span>
            )}
            {doc.expiresAt && (
              <span className="inline-flex items-center gap-1">
                <Clock className="h-3 w-3" /> {t('docs.expiresIn')} {daysLeft(doc.expiresAt)}
                {t('docs.days')}
              </span>
            )}
          </div>
          {doc.status === 'FAILED' && doc.errorMessage && <div className="mt-2 text-xs text-red-300">{doc.errorMessage}</div>}
        </div>
        {onDelete && (
          <button
            onClick={() => window.confirm(t('docs.confirmDelete')) && onDelete()}
            className="rounded-lg p-2 text-muted transition-colors hover:bg-danger/10 hover:text-danger"
            aria-label={t('docs.delete')}
            title={t('docs.delete')}
          >
            <Trash className="h-4 w-4" />
          </button>
        )}
      </div>
      {children}
    </motion.div>
  )
}
