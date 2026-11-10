export const MIN_CREDITS = 1
export const MAX_CREDITS = 32767

export function isValidCredits(credits: number | null): boolean {
  return credits === null || (
    Number.isInteger(credits) && credits >= MIN_CREDITS && credits <= MAX_CREDITS
  )
}
