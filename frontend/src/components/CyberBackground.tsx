import {lazy, Suspense, useSyncExternalStore} from 'react'
import {useSettings} from '../lib/settings'

const LetterGlitch = lazy(() => import('./letter-glitch/LetterGlitch'))
const BLUE_COLORS = ['#00b4d8', '#48cae4', '#90e0ef', '#ade8f4']
const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'

function subscribeToReducedMotion(onChange: () => void) {
  const query = window.matchMedia(REDUCED_MOTION_QUERY)
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

const getReducedMotion = () => window.matchMedia(REDUCED_MOTION_QUERY).matches
const getServerReducedMotion = () => true

/** Decorative motion stays inside the navy surfaces, behind the interface. */
export function CyberBackground({ variant = 'sidebar' }: { variant?: 'sign-in' | 'sidebar' }) {
  const { animations } = useSettings()
  const reducedMotion = useSyncExternalStore(subscribeToReducedMotion, getReducedMotion, getServerReducedMotion)

  // Unmount the canvas so disabling motion also releases its animation loop and GPU resources.
  if (!animations || reducedMotion) return null

  const signIn = variant === 'sign-in'
  return (
    <div className={`letter-glitch-background letter-glitch-background-${variant}`} aria-hidden="true">
      <Suspense fallback={null}>
        <LetterGlitch
          glitchColors={BLUE_COLORS}
          baseColor="#90e0ef"
          backgroundColor="#03045e"
          fontSize={signIn ? 16 : 14}
          spacing={0.6}
          density={signIn ? 0.95 : 0.85}
          baseOpacity={signIn ? 0.34 : 0.25}
          glitchAmount={signIn ? 0.55 : 0.4}
          speed={signIn ? 1 : 0.8}
          cursorRadius={signIn ? 160 : 100}
          clickEffect="tear"
          centerFade={signIn ? 0.3 : 0.12}
          edgeFade={0.04}
          flow={0.4}
          dpr={1}
        />
      </Suspense>
    </div>
  )
}
