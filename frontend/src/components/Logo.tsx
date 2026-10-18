import {ShieldCheck} from 'lucide-react'

export function Logo({ size = 'md' }: { size?: 'md' | 'lg' }) {
  const big = size === 'lg'
  return (
    <div className="flex items-center gap-3 select-none">
      <div
        className={`relative grid place-items-center rounded-xl border border-neon/40 bg-neon/10 shadow-[0_0_24px_-4px_rgb(34_211_238/0.7)] ${big ? 'h-14 w-14' : 'h-10 w-10'}`}
      >
        <ShieldCheck className={`text-neon ${big ? 'h-8 w-8' : 'h-6 w-6'}`} />
      </div>
      <span
        data-text="CyberLaw"
        className={`glitch font-display font-extrabold tracking-wider neon-text ${big ? 'text-4xl' : 'text-xl'}`}
      >
        CyberLaw
      </span>
    </div>
  )
}
