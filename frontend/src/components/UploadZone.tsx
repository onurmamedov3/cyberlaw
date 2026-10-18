import {useRef, useState} from 'react'
import {FileUp, LoaderCircle} from 'lucide-react'
import {upload} from '../lib/api'
import {useT} from '../lib/settings'
import type {DocScope, LibraryDocument} from '../lib/types'

const ACCEPT = '.pdf,.docx,.doc,.txt,.md,.html,.htm,.rtf,.odt'
const MAX_BYTES = 10 * 1024 * 1024

/** Drag & drop upload area. */
export function UploadZone({ scope, onUploaded }: { scope: DocScope; onUploaded: (doc: LibraryDocument) => void }) {
  const t = useT()
  const input = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const send = async (files: FileList | null) => {
    const file = files?.[0]
    if (!file) return
    setError(null)
    if (file.size > MAX_BYTES) {
      setError(t('docs.formats'))
      return
    }
    const form = new FormData()
    form.append('file', file)
    form.append('scope', scope)
    setBusy(true)
    try {
      onUploaded(await upload<LibraryDocument>('/documents', form))
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
      if (input.current) input.current.value = ''
    }
  }

  return (
    <div>
      <button
        type="button"
        disabled={busy}
        onClick={() => input.current?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          send(e.dataTransfer.files)
        }}
        className={`group relative flex w-full flex-col items-center justify-center gap-3 overflow-hidden rounded-2xl border-2 border-dashed px-6 py-10 transition-all ${
          dragging
            ? 'border-neon bg-neon/10 shadow-[0_0_40px_-10px_var(--color-neon)]'
            : 'border-line bg-panel/40 hover:border-neon/60 hover:bg-neon/5'
        }`}
      >
        {busy ? (
          <LoaderCircle className="h-10 w-10 animate-spin text-neon" />
        ) : (
          <FileUp className="h-10 w-10 text-neon transition-transform group-hover:-translate-y-1" />
        )}
        <div className="font-semibold">{busy ? t('docs.uploading') : t('docs.drop')}</div>
        <div className="text-xs text-muted">{t('docs.formats')}</div>
      </button>
      <input ref={input} type="file" accept={ACCEPT} hidden onChange={(e) => send(e.target.files)} />
      {error && <div className="mt-3 rounded-lg border border-danger/40 bg-danger/10 px-3 py-2 text-sm text-red-200">{error}</div>}
    </div>
  )
}
