import type {ReactNode} from 'react'
import {motion} from 'motion/react'

export function PageHeader({ icon, title, subtitle, children }: { icon: ReactNode; title: string; subtitle?: string; children?: ReactNode }) {
  return (
    <motion.div
      className="mb-8 flex flex-wrap items-start justify-between gap-4"
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className="flex items-start gap-4">
        <div className="grid h-12 w-12 shrink-0 place-items-center rounded-xl border border-neon/40 bg-neon/10 text-neon shadow-[0_0_30px_-8px_var(--color-neon)]">
          {icon}
        </div>
        <div>
          <h1 className="font-display text-2xl font-extrabold tracking-wide">{title}</h1>
          {subtitle && <p className="mt-1 max-w-2xl text-sm text-muted">{subtitle}</p>}
        </div>
      </div>
      {children}
    </motion.div>
  )
}
