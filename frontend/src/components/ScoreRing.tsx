import {motion} from 'motion/react'

export function scoreColor(score: number) {
  if (score >= 75) return 'var(--color-acid)'
  if (score >= 40) return 'var(--color-amber)'
  return 'var(--color-danger)'
}

/** Animated circular score, e.g. the compliance percentage. */
export function ScoreRing({ score, size = 160, label }: { score: number; size?: number; label?: string }) {
  const stroke = size / 14
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const color = scoreColor(score)
  return (
    <div className="relative" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={radius} stroke="var(--color-line)" strokeWidth={stroke} fill="none" />
        <motion.circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={color}
          strokeWidth={stroke}
          strokeLinecap="round"
          fill="none"
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: circumference * (1 - score / 100) }}
          transition={{ duration: 1.2, ease: 'easeOut' }}
        />
      </svg>
      <div className="absolute inset-0 grid place-items-center text-center">
        <div>
          <div className="font-display font-extrabold" style={{ color, fontSize: size / 4.2 }}>
            {score}%
          </div>
          {label && <div className="label-mono mt-1 !text-[0.6rem]">{label}</div>}
        </div>
      </div>
    </div>
  )
}
