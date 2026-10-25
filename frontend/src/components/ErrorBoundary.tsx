import {Component, type ErrorInfo, type ReactNode} from 'react'
import {RefreshCw, TriangleAlert} from 'lucide-react'
import {useT} from '../lib/settings'

interface State {
  crashed: boolean
}

/** Catches a crash while rendering, so one broken page shows a message instead of a blank app. */
export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { crashed: false }

  static getDerivedStateFromError(): State {
    return { crashed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Page crashed:', error, info.componentStack)
  }

  render() {
    return this.state.crashed ? <CrashScreen /> : this.props.children
  }
}

function CrashScreen() {
  const t = useT()
  return (
    <div className="grid min-h-full place-items-center p-6">
      <div role="alert" className="glass max-w-md p-8 text-center">
        <TriangleAlert className="mx-auto h-10 w-10 text-amber" />
        <h1 className="mt-4 text-xl font-bold">{t('common.error')}</h1>
        <p className="mt-2 text-sm text-muted">{t('common.crashHint')}</p>
        <button className="btn btn-neon mt-6" onClick={() => window.location.reload()}>
          <RefreshCw className="h-4 w-4" /> {t('common.reload')}
        </button>
      </div>
    </div>
  )
}
