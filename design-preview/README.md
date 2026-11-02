# CyberLaw blue palette review

Branch: `design/blue-workspace`

Working folder: the main CyberLaw project folder.

Open [the blue screenshot gallery](index.html). To compare the green version, switch to `design/legal-workspace` and open its gallery.

## Palette

| Colour | Role |
|---|---|
| `#03045E` | Navy navigation, sign-in feature panel, headings, button hover |
| `#023E8A` | Primary actions, links, active navigation background |
| `#0096C7` | Supporting accents and focus borders |
| `#00B4D8` | Page header rules and citation hover borders |
| `#48CAE4` | Sidebar action, active indicator, hover borders |
| `#90E0EF` | Input borders, scrollbar, secondary sidebar text |
| `#ADE8F4` | Sidebar links, source citation hover |
| `#CAF0F8` | Pale surfaces and text against navy |

The supplied list repeated `#023E8A`; all eight unique shades are represented. White and restrained blue tints keep reading surfaces light. Green, amber and red retain their existing success, warning and error meanings.

## Scope and preservation

The blue design is committed on `design/blue-workspace`; the green design is committed on `design/legal-workspace`. Both branches include a shared checkpoint of the pre-existing functionality. The original stash remains untouched as a backup. No settings features or dependencies were added.

Switch normally from the main project folder with `git switch design/legal-workspace` or `git switch design/blue-workspace`. Run `npm.cmd run dev` in `frontend`; its API proxy points to the existing backend on port 8080. The gallery opens directly from disk.

## Composer refinements

The search scope sits inside the left edge of the composer, next to the writing area; Send stays on the right. On narrow screens the scope keeps its icon, dropdown arrow and accessible label while leaving room for typing.

Focusing the writing area changes the existing outer border colour. The duplicate inner outline and outer focus halo are removed; keyboard focus indicators remain on the other controls.

Focused browser checks: [composer-verification.json](composer-verification.json).

## Validation

- `npm.cmd run typecheck` passes.
- `npm.cmd run build` passes, with Vite's bundle-size advisory for the existing main JavaScript chunk.
- [Browser results](verification.json): registration fields, login payload, search scope options, streamed answers, citations, compliance navigation, report filters, mobile navigation and phone-width overflow checks.
- Twenty-two screenshots cover desktop and mobile, including English and Azerbaijani.

Screenshots render the actual React interface using isolated sample API responses. No real account, documents, or backend records were changed. External fonts were disabled during capture, so the configured system fallbacks appear. Live backend and AI calls were not retested.
