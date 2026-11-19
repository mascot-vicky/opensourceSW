import type { CSSProperties } from 'react'
import { IconArrowRightLine, IconArrowLeftLine, IconMagnifyingglassLine, IconPersonLine, IconLocationpinLine, IconXmarkLine, IconCrosshairLine, IconPlusLine, IconMinusLine, IconCheckmarkLine, IconCupHeatwaveLine, IconBracketLeftArrowRightLine, IconArrowClockwiseCircularLine, IconCheckmarkClipboardLine, IconArrowUpRightLine, IconChevronRightLine } from '@karrotmarket/react-monochrome-icon'

export type IconName = 'arrow' | 'back' | 'search' | 'user' | 'pin' | 'close' | 'location' | 'plus' | 'minus' | 'check' | 'cup' | 'logout' | 'reset' | 'copy' | 'external' | 'chevron'
const icons = {
  arrow: IconArrowRightLine, back: IconArrowLeftLine, search: IconMagnifyingglassLine,
  user: IconPersonLine, pin: IconLocationpinLine, close: IconXmarkLine, location: IconCrosshairLine,
  plus: IconPlusLine, minus: IconMinusLine, check: IconCheckmarkLine, cup: IconCupHeatwaveLine,
  logout: IconBracketLeftArrowRightLine, reset: IconArrowClockwiseCircularLine,
  copy: IconCheckmarkClipboardLine, external: IconArrowUpRightLine, chevron: IconChevronRightLine,
}
export function Glyph({ name, size = 22, style }: { name: IconName; size?: number; style?: CSSProperties }) {
  const Component = icons[name]
  return <Component aria-hidden="true" width={size} height={size} style={style} />
}
