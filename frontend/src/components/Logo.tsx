import {Scale} from 'lucide-react'

export function Logo({ size = 'md' }: { size?: 'md' | 'lg' }) {
  const big = size === 'lg'
  return (
    <div className="flex select-none items-center gap-2.5">
      <span className={`grid shrink-0 place-items-center rounded-md bg-neon text-white ${big ? 'h-11 w-11' : 'h-8 w-8'}`}>
        <Scale className={big ? 'h-6 w-6' : 'h-5 w-5'} strokeWidth={1.5} />
      </span>
      <span className={`font-display font-semibold tracking-tight text-ink ${big ? 'text-3xl' : 'text-[23px]'}`}>
        CyberLaw<span className="text-neon">.</span>
      </span>
    </div>
  )
}
