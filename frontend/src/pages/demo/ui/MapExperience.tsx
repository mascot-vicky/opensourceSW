import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { mockCafes, mockMenus, mockUser } from '../mock'
import { formatPrice } from '../model/demo'
import { Glyph } from './Icons'
import { Brand } from './Onboarding'
import { DemoMap } from './DemoMap'
import { CafeSheet } from './CafeSheet'

export function MapExperience({ onResurvey, onLogout }: { onResurvey: () => void; onLogout: () => void }) {
  const [query, setQuery] = useState('')
  const [exploring, setExploring] = useState(false)
  const [activeMenuId, setActiveMenuId] = useState(mockMenus[0].id)
  const [selectedCafeId, setSelectedCafeId] = useState<string | null>(null)
  const [sheetMenuId, setSheetMenuId] = useState<string | null>(null)
  const [sheetOpen, setSheetOpen] = useState(false)
  const [toast, setToast] = useState('')
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const account = useRef<HTMLDetailsElement>(null)
  const carousel = useRef<HTMLDivElement>(null)
  const currentMenu = useRef(mockMenus[0].id)
  const returnTo = useRef<{ menuId: string; scrollLeft: number; element: HTMLElement | null } | null>(null)
  const cardElements = useRef(new Map<string, HTMLButtonElement>())
  const normalizedQuery = query.trim().toLocaleLowerCase('ko-KR')
  const cafes = useMemo(() => mockCafes.filter(cafe => cafe.name.toLocaleLowerCase('ko-KR').includes(normalizedQuery) || mockMenus.some(menu => menu.cafeId === cafe.id && menu.name.toLocaleLowerCase('ko-KR').includes(normalizedQuery))), [normalizedQuery])
  const menus = useMemo(() => mockMenus.filter(menu => menu.availability !== 'SEASON_ENDED' && (menu.name.toLocaleLowerCase('ko-KR').includes(normalizedQuery) || mockCafes.find(cafe => cafe.id === menu.cafeId)?.name.toLocaleLowerCase('ko-KR').includes(normalizedQuery))), [normalizedQuery])
  const activeMenu = menus.find(menu => menu.id === activeMenuId) ?? menus[0]
  const activeIndex = menus.findIndex(menu => menu.id === activeMenu?.id)
  const activeCafeId = selectedCafeId ?? (exploring ? activeMenu?.cafeId ?? null : null)
  const notify = useCallback((message: string) => {
    if (timer.current) clearTimeout(timer.current)
    setToast(message)
    timer.current = setTimeout(() => setToast(''), 5000)
  }, [])
  useEffect(() => { if (activeMenu) currentMenu.current = activeMenu.id }, [activeMenu])
  const selectCafe = useCallback((id: string) => {
    returnTo.current = { menuId: currentMenu.current, scrollLeft: carousel.current?.scrollLeft ?? 0, element: document.activeElement instanceof HTMLElement ? document.activeElement : null }
    setToast(''); setSheetMenuId(null); setSelectedCafeId(id); setSheetOpen(true)
  }, [])
  useEffect(() => {
    function dismiss(event: PointerEvent) {
      if (event.target instanceof Node && !account.current?.contains(event.target) && account.current) account.current.open = false
    }
    document.addEventListener('pointerdown', dismiss)
    return () => { document.removeEventListener('pointerdown', dismiss); if (timer.current) clearTimeout(timer.current) }
  }, [])

  function moveMenu(index: number) {
    const menu = menus[index]
    if (!menu) return
    setActiveMenuId(menu.id)
    cardElements.current.get(menu.id)?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'nearest', inline: 'center' })
  }
  function restoreExploration() {
    const previous = returnTo.current
    setSelectedCafeId(null)
    if (previous) {
      setActiveMenuId(previous.menuId)
      requestAnimationFrame(() => {
        previous.element?.focus({ preventScroll: true })
        carousel.current?.scrollTo({ left: previous.scrollLeft, behavior: 'instant' })
      })
    }
  }
  function updateActiveCard() {
    if (selectedCafeId) return
    const viewport = carousel.current
    if (!viewport || !menus.length) return
    const center = viewport.scrollLeft + viewport.clientWidth / 2
    let closest = menus[0].id
    let distance = Infinity
    for (const menu of menus) {
      const card = cardElements.current.get(menu.id)
      if (!card) continue
      const next = Math.abs(card.offsetLeft + card.offsetWidth / 2 - center)
      if (next < distance) { closest = menu.id; distance = next }
    }
    setActiveMenuId(closest)
  }
  return <main className={`map-experience${exploring ? ' exploring' : ''}${selectedCafeId ? ' has-sheet' : ''}`}>
    <DemoMap cafes={cafes} activeCafeId={activeCafeId} exploring={exploring} onSelect={selectCafe} notify={notify} />
    <div className="map-wordmark"><Brand /></div>
    <header className="map-topbar">
      {exploring && <button type="button" className="map-top-button" aria-label="지도 메인으로 돌아가기" onClick={() => setExploring(false)}><Glyph name="back" /></button>}
      <div className="map-search"><Glyph name="search" size={21} /><input type="search" aria-label="카페 또는 메뉴 검색" placeholder="카페 또는 메뉴 검색" value={query} onChange={event => setQuery(event.target.value)} />{query && <button type="button" className="icon-button" aria-label="검색어 지우기" onClick={() => setQuery('')}><Glyph name="close" size={17} /></button>}</div>
      <details className="account-menu" ref={account} onKeyDown={event => { if (event.key === 'Escape' && account.current) { account.current.open = false; account.current.querySelector('summary')?.focus() } }}><summary aria-label="마이페이지"><Glyph name="user" size={23} /></summary><div className="account-popover"><p><strong>{mockUser.displayName}님</strong></p><button type="button" onClick={onResurvey}><Glyph name="reset" size={19} />설문 다시하기<Glyph name="chevron" size={16} /></button><button type="button" onClick={onLogout}><Glyph name="logout" size={19} />로그아웃</button></div></details>
    </header>
    <div className="map-area-label"><span className="area-dot" />행궁동 <span className="area-separator">/</span> 카페 {cafes.length}곳</div>
    {!cafes.length && <div className="map-no-results"><Glyph name="search" size={25} /><strong>찾는 카페가 없어요</strong><span>다른 카페명이나 메뉴로 찾아보세요.</span><button type="button" className="text-button" onClick={() => setQuery('')}>전체 카페 보기</button></div>}
    {exploring ? <section className="menu-explorer" aria-label="메뉴 탐색">
      {menus.length ? <><div className="menu-carousel" ref={carousel} onScrollEnd={updateActiveCard}>{menus.map(menu => <button type="button" key={menu.id} ref={node => { if (node) cardElements.current.set(menu.id, node); else cardElements.current.delete(menu.id) }} className={`menu-card${menu.id === activeMenu?.id ? ' is-active' : ''}`} aria-label={`${menu.name} 자세히 보기`} aria-current={menu.id === activeMenu?.id ? 'true' : undefined} onClick={event => {
          const card = cardElements.current.get(menu.id)
          returnTo.current = { menuId: menu.id, scrollLeft: card && carousel.current ? card.offsetLeft + card.offsetWidth / 2 - carousel.current.clientWidth / 2 : 0, element: event.currentTarget }
          setToast(''); setActiveMenuId(menu.id); setSheetMenuId(menu.id); setSelectedCafeId(menu.cafeId); setSheetOpen(true)
        }}>
        <div className={`menu-card-art menu-art art-${menu.id}`}><img src={menu.imageUrl} alt={menu.imageAlt} /></div><div className="menu-card-copy"><span className="menu-cafe-name">{mockCafes.find(cafe => cafe.id === menu.cafeId)?.name}</span><div><h2>{menu.name}</h2><Glyph name="chevron" size={18} /></div><p>{menu.description}</p><div className="menu-card-bottom"><strong>{formatPrice(menu.priceKrw)}</strong><span>{menu.demoTags.join(' · ')}</span></div></div>
      </button>)}</div><div className="carousel-navigation"><button type="button" aria-label="이전 메뉴" disabled={activeIndex <= 0} onClick={() => moveMenu(activeIndex - 1)}><Glyph name="back" size={18} /></button><span className="menu-position" aria-live="polite">{menus.length ? activeIndex + 1 : 0}<span> / {menus.length}</span></span><button type="button" aria-label="다음 메뉴" disabled={activeIndex >= menus.length - 1} onClick={() => moveMenu(activeIndex + 1)}><Glyph name="arrow" size={18} /></button></div></> : <div className="empty-menu-card">검색에 맞는 메뉴가 없어요.<button type="button" className="text-button" onClick={() => setQuery('')}>검색 초기화</button></div>}
    </section> : <div className="explore-launch"><button type="button" className="explore-orb" aria-label="메뉴 탐색 시작" onClick={() => setExploring(true)}><Glyph name="cup" size={30} /><span className="orb-arrow"><Glyph name="arrow" size={13} /></span></button><span className="explore-label">메뉴 탐색</span></div>}
    <CafeSheet cafe={mockCafes.find(cafe => cafe.id === selectedCafeId)} selectedMenuId={sheetMenuId} open={sheetOpen} onClose={() => setSheetOpen(false)} onExited={restoreExploration} notify={notify} />
    <div className={`demo-toast${toast ? ' visible' : ''}`} role="status" aria-live="polite">{toast}</div>
  </main>
}
