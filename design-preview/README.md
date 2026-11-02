# CyberLaw design review

Branch: `design/legal-workspace`

Open [the screenshot gallery](index.html), then click any image to inspect the full-size screenshot.

## Direction

- Warm off-white canvas, solid white panels, forest green actions and subdued status colours.
- Serif headings paired with readable interface text; smaller, consistent corner radii.
- A full-height navigation rail, clear active states, and a breadcrumb with the existing language switch.
- Left-aligned research prompts, simpler suggestion rows, and a writing area with search scope and send controls underneath.
- Matching document, compliance, report, settings, admin and authentication screens.
- Responsive mobile layouts and a light print layout for reports.

The existing API calls, authentication, chat history, streaming, document uploads, compliance checks, report filters, citations, administration and language settings remain in place. Backend source and dependency versions were not changed by this redesign. Uncommitted work present before the redesign was preserved. The green design is committed on `design/legal-workspace`; the blue variant is on `design/blue-workspace`. Switch branches in the main project folder to compare them.

## Preview data

All screenshots show the actual React components with isolated sample API responses in a temporary browser profile. They do not use or change real account records. External font requests were disabled during capture, so screenshots use the configured system fallbacks; the app also requests Inter and Source Serif 4 when available.

## Validation

- `npm.cmd run typecheck`
- `npm.cmd run build`
- Browser interaction checks and mobile layout checks: [verification.json](verification.json).

Browser checks cover registration fields, login submission, scope options, streamed chat rendering, citations, compliance navigation, verdict filtering, mobile navigation, and horizontal overflow at phone width. These are frontend checks with sample responses; live backend and AI behaviour were not retested.

The production build succeeds with Vite's bundle-size advisory (the main JavaScript chunk exceeds 500 kB).

## Local application

From `frontend`, run `npm.cmd run dev`. The existing backend remains on port 8080 and the Vite frontend on port 5173. The gallery can be opened directly from disk without running either server.
