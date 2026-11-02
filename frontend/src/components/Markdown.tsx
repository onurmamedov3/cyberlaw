import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

const CITATION = /\[(\d{1,2})\](?!\()/g

/** Turns "[2]" into a link the renderer below displays as a clickable citation chip. */
function linkCitations(text: string) {
  return text.replace(CITATION, '[$1](#cite-$1)')
}

/** The source numbers an answer actually cites, e.g. "… [1] … [2][3]" → {1, 2, 3}. */
export function citedIndexes(text: string): Set<number> {
  return new Set(Array.from(text.matchAll(CITATION), (m) => Number(m[1])))
}

/** The host of an absolute http(s) link, or null for anything else (relative, mailto:, javascript:, …). */
function webHost(href: string | undefined) {
  try {
    const url = new URL(href ?? '')
    return url.protocol === 'https:' || url.protocol === 'http:' ? url.host : null
  } catch {
    return null
  }
}

/**
 * Renders an answer. The text comes from the model, which may have been manipulated by a document, so:
 * no raw HTML (react-markdown's default), no images (an image URL would leak data the moment it loads),
 * and links only to http(s) sites, with the real domain shown next to the link text.
 */
export function Markdown({ text, onCite }: { text: string; onCite?: (index: number) => void }) {
  return (
    <div className="answer prose prose-invert prose-sm sm:prose-base">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        disallowedElements={['img']}
        components={{
          a: ({ href, children }) => {
            if (href?.startsWith('#cite-')) {
              const index = Number(href.slice(6))
              return (
                <button type="button" className="cite" onClick={() => onCite?.(index)}>
                  {index}
                </button>
              )
            }
            const host = webHost(href)
            if (!host) return <span>{children}</span>
            return (
              <>
                <a href={href} target="_blank" rel="noopener noreferrer nofollow">
                  {children}
                </a>
                {!(typeof children === 'string' && children.includes(host)) && (
                  <span className="text-xs text-muted"> ({host})</span>
                )}
              </>
            )
          },
        }}
      >
        {linkCitations(text)}
      </ReactMarkdown>
    </div>
  )
}
