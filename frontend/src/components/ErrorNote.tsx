import {useErrorText} from '../lib/errors'

/** A red box with the translated text of an error; renders nothing when there is no error. */
export function ErrorNote({ error, className = '' }: { error: unknown; className?: string }) {
  const errorText = useErrorText()
  if (!error) return null
  return (
    <div role="alert" className={`rounded-lg border border-danger/40 bg-danger/10 px-3 py-2 text-sm text-danger ${className}`}>
      {errorText(error)}
    </div>
  )
}
