import {useSettings} from '../lib/settings'

/** Glowing orbs + perspective grid + subtle scanlines behind every page. */
export function CyberBackground() {
  const { animations } = useSettings()
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      <div className="cyber-glow" style={{ width: 520, height: 520, left: '-12%', top: '-18%', background: '#22d3ee' }} />
      <div
        className="cyber-glow"
        style={{ width: 460, height: 460, right: '-10%', bottom: '-12%', background: '#a855f7', animationDelay: '-7s' }}
      />
      {animations && <div className="cyber-grid" />}
      <div className="scanline" />
    </div>
  )
}
