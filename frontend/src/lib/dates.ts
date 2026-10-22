/** Whole days until a date (e.g. a private document's automatic deletion), never below 0. */
export function daysLeft(date: string) {
  return Math.max(0, Math.ceil((new Date(date).getTime() - Date.now()) / 86_400_000))
}
