import {AnimatePresence, motion} from 'motion/react'
import {BookOpen, FolderLock, Landmark, X} from 'lucide-react'
import {useT} from '../lib/settings'
import type {Source} from '../lib/types'

/** Slide-in panel that shows the exact passage behind a citation. */
export function SourceDrawer({ source, onClose }: { source: Source | null; onClose: () => void }) {
  const t = useT()
  return (
    <AnimatePresence>
      {source && (
        <>
          <motion.div
            className="fixed inset-0 z-40 bg-black/50"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.aside
            className="glass fixed inset-y-3 right-3 z-50 flex w-[min(560px,calc(100vw-1.5rem))] flex-col overflow-hidden"
            initial={{ x: '110%' }}
            animate={{ x: 0 }}
            exit={{ x: '110%' }}
            transition={{ type: 'spring', damping: 28, stiffness: 260 }}
          >
            <div className="flex items-start justify-between gap-4 border-b border-line p-5">
              <div className="min-w-0">
                <div className="label-mono mb-2 flex items-center gap-2">
                  <span className="cite">{source.index}</span> {t('source.passage')}
                </div>
                <h3 className="text-lg font-semibold leading-snug">{source.documentTitle}</h3>
                <div className="mt-2 flex flex-wrap gap-2">
                  <span className={`chip ${source.scope === 'OFFICIAL' ? 'text-neon' : 'text-violet'}`}>
                    {source.scope === 'OFFICIAL' ? <Landmark className="h-3 w-3" /> : <FolderLock className="h-3 w-3" />}
                    {source.scope === 'OFFICIAL' ? t('source.official') : t('source.private')}
                  </span>
                  {source.article && (
                    <span className="chip text-acid">
                      <BookOpen className="h-3 w-3" />
                      {source.article}
                    </span>
                  )}
                </div>
              </div>
              <button onClick={onClose} className="btn btn-ghost px-2.5" aria-label={t('common.close')}>
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-5">
              {source.chapter && <div className="label-mono mb-2">{source.chapter}</div>}
              {source.articleTitle && (
                <div className="mb-3 border-l-2 border-neon pl-3 font-semibold text-neon-soft">
                  {source.article}. {source.articleTitle}
                </div>
              )}
              <p className="whitespace-pre-wrap font-mono text-[13px] leading-relaxed text-slate-300">{source.text}</p>
            </div>

            <div className="border-t border-line p-5">
              <div className="label-mono mb-2 flex justify-between">
                <span>{t('source.relevance')}</span>
                <span className="text-neon">{Math.round(source.score * 100)}%</span>
              </div>
              <div className="h-1.5 overflow-hidden rounded-full bg-line">
                <motion.div
                  className="h-full rounded-full bg-gradient-to-r from-neon to-violet"
                  initial={{ width: 0 }}
                  animate={{ width: `${Math.round(source.score * 100)}%` }}
                  transition={{ duration: 0.8 }}
                />
              </div>
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  )
}
