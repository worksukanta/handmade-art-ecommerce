import { useEffect, type RefObject } from 'react'

/** Include both the trigger and panel so trigger clicks retain normal toggling. */
export function useDismiss(
  boundaries: RefObject<HTMLElement | null>[],
  enabled: boolean,
  onDismiss: (escape: boolean) => void,
) {
  useEffect(() => {
    if (!enabled) return
    const pointerDown = (event: PointerEvent) => {
      if (event.target instanceof Node && !boundaries.some((ref) => ref.current?.contains(event.target as Node))) onDismiss(false)
    }
    const keyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !event.defaultPrevented) onDismiss(true)
    }
    document.addEventListener('pointerdown', pointerDown)
    document.addEventListener('keydown', keyDown)
    return () => {
      document.removeEventListener('pointerdown', pointerDown)
      document.removeEventListener('keydown', keyDown)
    }
  }, [boundaries, enabled, onDismiss])
}
