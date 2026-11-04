import type {ReactNode} from 'react'
import {motion} from 'motion/react'

export function PageHeader({ icon, title, subtitle, children }: { icon: ReactNode; title: string; subtitle?: string; children?: ReactNode }) {
  return (
    <motion.div className="page-header flex flex-wrap items-start justify-between gap-4" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }}>
      <div className="min-w-0">
        <div className="flex items-center gap-3">
          <span className="shrink-0 text-neon" aria-hidden>{icon}</span>
          <h1 className="page-heading min-w-0 break-words">{title}</h1>
        </div>
        {subtitle && <p className="mt-3 max-w-2xl text-sm leading-relaxed text-muted">{subtitle}</p>}
      </div>
      {children}
    </motion.div>
  )
}
