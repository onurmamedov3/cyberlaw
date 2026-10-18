import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

/** Turns "[2]" into a link the renderer below displays as a clickable citation chip. */
function linkCitations(text: string) {
  return text.replace(/\[(\d{1,2})\](?!\()/g, '[$1](#cite-$1)')
}

export function Markdown({ text, onCite }: { text: string; onCite?: (index: number) => void }) {
  return (
    <div className="answer prose prose-invert prose-sm sm:prose-base">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
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
            return (
              <a href={href} target="_blank" rel="noreferrer">
                {children}
              </a>
            )
          },
        }}
      >
        {linkCitations(text)}
      </ReactMarkdown>
    </div>
  )
}
