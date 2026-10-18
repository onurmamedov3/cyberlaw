import {CircleCheck, CircleX, LoaderCircle} from 'lucide-react'
import {useT} from '../lib/settings'
import type {DocStatus} from '../lib/types'

export function StatusBadge({ status }: { status: DocStatus | 'RUNNING' | 'DONE' }) {
  const t = useT()
  const busy = status === 'PROCESSING' || status === 'RUNNING'
  const ok = status === 'READY' || status === 'DONE'
  const color = busy ? 'text-amber' : ok ? 'text-acid' : 'text-danger'
  const Icon = busy ? LoaderCircle : ok ? CircleCheck : CircleX
  return (
    <span className={`chip ${color}`}>
      <Icon className={`h-3 w-3 ${busy ? 'animate-spin' : ''}`} />
      {t(`status.${status}`)}
    </span>
  )
}
