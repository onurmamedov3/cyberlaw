import {lazy, Suspense, useSyncExternalStore} from 'react'
import {useSettings} from '../lib/settings'

const LetterGlitch = lazy(() => import('./letter-glitch/LetterGlitch'))
const BLUE_COLORS = ['#0096c7', '#00b4d8', '#48cae4', '#90e0ef']
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
          fontSize={signIn ? 14 : 12}
          spacing={0.8}
          density={signIn ? 0.8 : 0.65}
          baseOpacity={signIn ? 0.13 : 0.08}
          glitchAmount={signIn ? 0.3 : 0.18}
          speed={signIn ? 0.7 : 0.45}
          cursorRadius={signIn ? 120 : 70}
          clickEffect="tear"
          centerFade={signIn ? 0.65 : 0.35}
          edgeFade={0.08}
          flow={0.3}
          dpr={1}
        />
      </Suspense>
    </div>
  )
}
