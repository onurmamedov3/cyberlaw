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
      {/* shared cyan→violet gradient for icons: <Sparkles color="url(#cl-gradient)" /> */}
      <svg width="0" height="0" className="absolute">
        <defs>
          <linearGradient id="cl-gradient" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#22d3ee" />
            <stop offset="100%" stopColor="#a855f7" />
          </linearGradient>
        </defs>
      </svg>
    </div>
  )
}
