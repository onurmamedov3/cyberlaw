import {createContext, type ReactNode, useCallback, useContext, useEffect, useRef, useState} from 'react'
import {AnimatePresence, motion} from 'motion/react'
import {TriangleAlert} from 'lucide-react'
import {useT} from '../lib/settings'

interface ConfirmOptions {
  message: string
  confirmLabel: string
}

type Confirm = (options: ConfirmOptions) => Promise<boolean>

interface Request extends ConfirmOptions {
  resolve: (confirmed: boolean) => void
}

const ConfirmContext = createContext<Confirm | null>(null)

/** A styled replacement for window.confirm(): `if (await confirm({ message, confirmLabel })) …` */
export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [request, setRequest] = useState<Request | null>(null)

  const confirm = useCallback<Confirm>((options) => new Promise((resolve) => setRequest({ ...options, resolve })), [])

  const close = (confirmed: boolean) => {
    request?.resolve(confirmed)
    setRequest(null)
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <AnimatePresence>{request && <Dialog request={request} onClose={close} />}</AnimatePresence>
    </ConfirmContext.Provider>
  )
}

function Dialog({ request, onClose }: { request: Request; onClose: (confirmed: boolean) => void }) {
  const t = useT()
  const cancel = useRef<HTMLButtonElement>(null)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  // the safe choice has the focus; Escape cancels
  useEffect(() => {
    cancel.current?.focus()
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onCloseRef.current(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  return (
    <motion.div
      className="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={() => onClose(false)}
    >
      <motion.div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-message"
        className="glass w-full max-w-sm p-6 shadow-[0_0_60px_-20px_var(--color-danger)]"
        initial={{ scale: 0.95, y: 8 }}
        animate={{ scale: 1, y: 0 }}
        exit={{ scale: 0.95, y: 8 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start gap-3">
          <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-danger/40 bg-danger/10 text-danger">
            <TriangleAlert className="h-5 w-5" />
          </span>
          <p id="confirm-message" className="pt-2 font-semibold">
            {request.message}
          </p>
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <button ref={cancel} className="btn btn-ghost" onClick={() => onClose(false)}>
            {t('common.cancel')}
          </button>
          <button className="btn btn-danger" onClick={() => onClose(true)}>
            {request.confirmLabel}
          </button>
        </div>
      </motion.div>
    </motion.div>
  )
}

export function useConfirm() {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm must be used inside ConfirmProvider')
  return ctx
}
