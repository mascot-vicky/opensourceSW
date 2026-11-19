import { Text } from '@seed-design/react'
import { ActionButton, BottomSheetRoot, BottomSheetContent, BottomSheetBody, BottomSheetFooter } from '../../../shared/ui'
import { mockMenus, type DemoCafe } from '../mock'
import { directionsUrl, formatPrice } from '../model/demo'
import { Glyph } from './Icons'

export function CafeSheet({ cafe, selectedMenuId, open, onClose, onExited, notify }: {
  cafe: DemoCafe | undefined
  selectedMenuId: string | null
  open: boolean
  onExited: () => void
  onClose: () => void
  notify: (message: string) => void
}) {
  const menus = mockMenus.filter(menu => menu.cafeId === cafe?.id)
  const selected = menus.find(menu => menu.id === selectedMenuId) ?? menus[0]
  async function copyAddress() {
    if (!cafe) return
    try {
      await navigator.clipboard.writeText(cafe.roadAddress)
      notify('주소를 복사했어요.')
    } catch { notify('주소를 복사하지 못했어요. 주소를 길게 눌러 복사해주세요.') }
  }
  return <BottomSheetRoot open={open} onOpenChange={value => { if (!value) onClose() }} onAnimationEnd={value => { if (!value) onExited() }}>
    {cafe && <BottomSheetContent className="cafe-sheet" title={cafe.name} description="행궁동 · 가상 시연 카페" showHandle>
      <BottomSheetBody className="cafe-sheet-body">
        <div className="sheet-address"><Glyph name="pin" size={19} /><span>{cafe.roadAddress}</span><button type="button" className="icon-button" aria-label="주소 복사" onClick={copyAddress}><Glyph name="copy" size={19} /></button></div>
        {selected && <div className="sheet-featured">
          <div className={`menu-art art-${selected.id}`}><img src={selected.imageUrl} alt={selected.imageAlt} /></div>
          <div><Text as="h3" textStyle="t7Bold">{selected.name}</Text><p>{selected.description}</p><strong>{formatPrice(selected.priceKrw)}</strong><div className="taste-tags">{selected.demoTags.map(tag => <span key={tag}>{tag}</span>)}</div></div>
        </div>}
        <div className="sheet-menu-heading"><Text as="h3" textStyle="t6Bold">메뉴</Text><span>{menus.length}개</span></div>
        <ul className="sheet-menu-list">{menus.map(menu => <li key={menu.id} className={menu.id === selected?.id ? 'selected-menu' : ''}>
          <img src={menu.imageUrl} alt="" /><div><strong>{menu.name}</strong><span>{menu.availability === 'SEASON_ENDED' ? '시즌 종료' : '시연 메뉴 · 판매 여부 미확인'}</span></div><span>{formatPrice(menu.priceKrw)}</span>
        </li>)}</ul>
        <p className="sheet-demo-note">카페와 메뉴는 시연용 가상 자료예요. 지도 버튼은 표시된 시연 지점으로 연결돼요.</p>
      </BottomSheetBody>
      <BottomSheetFooter className="cafe-sheet-footer">
        <ActionButton asChild variant="neutralOutline"><a href={directionsUrl('naver', cafe)} target="_blank" rel="noopener noreferrer"><span className="map-provider naver-provider" aria-hidden="true">N</span>네이버 지도<Glyph name="external" size={16} /></a></ActionButton>
        <ActionButton asChild variant="neutralOutline"><a href={directionsUrl('kakao', cafe)} target="_blank" rel="noopener noreferrer"><span className="map-provider kakao-provider" aria-hidden="true"><Glyph name="pin" size={17} /></span>카카오 지도<Glyph name="external" size={16} /></a></ActionButton>
      </BottomSheetFooter>
    </BottomSheetContent>}
  </BottomSheetRoot>
}
