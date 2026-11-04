import {useRef, useState} from 'react'
import {Ban, FileUp, LoaderCircle} from 'lucide-react'
import {upload} from '../lib/api'
import {useAppConfig} from '../lib/config'
import {useT} from '../lib/settings'
import type {DocScope, LibraryDocument} from '../lib/types'
import {ErrorNote} from './ErrorNote'

const ACCEPT = '.pdf,.docx,.doc,.txt,.md,.html,.htm,.rtf,.odt'

/** Drag & drop upload area. `fullText`: the upload limit is reached, so the area is disabled with this note. */
export function UploadZone({
  scope,
  onUploaded,
  fullText,
}: {
  scope: DocScope
  onUploaded: (doc: LibraryDocument) => void
  fullText?: string
}) {
  const t = useT()
  const config = useAppConfig()
  const input = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)

  const send = async (files: FileList | null) => {
    const file = files?.[0]
    if (!file) return
    setError(null)
    // checked here too, so a large file is not uploaded just to be rejected
    if (config && file.size > config.maxFileSizeMb * 1024 * 1024) {
      setError({ code: 'TOO_LARGE', params: { maxMb: config.maxFileSizeMb } })
      return
    }
    const form = new FormData()
    form.append('file', file)
    form.append('scope', scope)
    setBusy(true)
    try {
      onUploaded(await upload<LibraryDocument>('/documents', form))
    } catch (e) {
      setError(e)
    } finally {
      setBusy(false)
      if (input.current) input.current.value = ''
    }
  }

  return (
    <div>
      <button
        type="button"
        disabled={busy || !!fullText}
        onClick={() => input.current?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          if (!fullText) setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          if (!fullText) send(e.dataTransfer.files)
        }}
        className={`upload-zone group relative flex w-full flex-col items-center justify-center gap-3 overflow-hidden rounded-lg border border-dashed px-6 py-10 transition-all ${
          fullText
            ? 'cursor-not-allowed border-line bg-panel/20 opacity-70'
            : dragging
              ? 'border-neon bg-neon/10'
              : 'border-line bg-panel/40 hover:border-neon/60 hover:bg-neon/5'
        }`}
      >
        {fullText ? (
          <Ban className="h-7 w-7 text-amber" />
        ) : busy ? (
          <LoaderCircle className="h-7 w-7 animate-spin text-neon" />
        ) : (
          <FileUp className="h-7 w-7 text-neon transition-transform group-hover:-translate-y-1" />
        )}
        <div className="font-semibold">{fullText ?? (busy ? t('docs.uploading') : t('docs.drop'))}</div>
        {config && !fullText && <div className="text-xs text-muted">{t('docs.formats', { mb: config.maxFileSizeMb })}</div>}
      </button>
      <input ref={input} type="file" accept={ACCEPT} hidden onChange={(e) => send(e.target.files)} />
      <ErrorNote error={error} className="mt-3" />
    </div>
  )
}
