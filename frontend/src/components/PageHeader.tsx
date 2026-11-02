import type {ReactNode} from 'react'
import {motion} from 'motion/react'

export function PageHeader({ icon, title, subtitle, children }: { icon: ReactNode; title: string; subtitle?: string; children?: ReactNode }) {
  return (
    <motion.div className="page-header flex flex-wrap items-start justify-between gap-4" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }}>
      <div className="min-w-0">
        <div className="mb-4 text-neon" aria-hidden>{icon}</div>
        <h1 className="page-heading">{title}</h1>
        {subtitle && <p className="mt-3 max-w-2xl text-sm leading-relaxed text-muted">{subtitle}</p>}
      </div>
      {children}
    </motion.div>
  )
}
