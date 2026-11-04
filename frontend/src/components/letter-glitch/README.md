# Letter Glitch

Vendored from React Bits by David Haz on 2026-11-02.

- Demo: https://reactbits.dev/backgrounds/letter-glitch
- TypeScript source: https://github.com/DavidHDev/react-bits/blob/main/src/ts-default/Backgrounds/LetterGlitch/LetterGlitch.tsx
- Styles: https://github.com/DavidHDev/react-bits/blob/main/src/ts-default/Backgrounds/LetterGlitch/LetterGlitch.css
- Licence: [LICENSE.md](LICENSE.md), also shipped in `frontend/public/licenses/react-bits.md`.

The upstream effect is retained. One cleanup adjustment defers context release until the canvas is detached, so React StrictMode's development effect remount does not lose its WebGL context. CyberLaw's `CyberBackground` wrapper supplies the blue palette, adjusts density and intensity for each navy surface, and unmounts the effect when animations are disabled or the device requests reduced motion. The canvas is decorative and does not intercept input. No extra package dependencies are required.
