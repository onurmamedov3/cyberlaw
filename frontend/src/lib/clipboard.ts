/**
 * Copies text. The Clipboard API only exists on https and localhost, so on plain http
 * (e.g. the app opened from a phone via the laptop's IP address) a hidden textarea is used.
 */
export async function copyText(text: string) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }
  const area = document.createElement('textarea')
  area.value = text
  area.setAttribute('readonly', '')
  area.style.position = 'fixed'
  area.style.opacity = '0'
  document.body.appendChild(area)
  area.select()
  document.execCommand('copy')
  area.remove()
}
