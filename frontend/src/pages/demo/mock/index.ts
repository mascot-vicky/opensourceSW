// 발표용 상수입니다. 실제 계정·매장 정보 또는 서버 API 응답을 대신하지 않습니다.
export const mockUser = { id: 'demo-user', displayName: '여행자' } as const

export type Question = {
  id: string
  label: string
  description: string
  type: 'single' | 'multiple'
  minSelections: number
  maxSelections: number
  options: { id: string; label: string; symbol: string }[]
}

export const mockQuestionnaire: { version: string; questions: Question[] } = {
  version: 'demo-questionnaire-v1',
  questions: [
    { id: 'kind', label: '메뉴 종류를\n선택해주세요', description: '1개 선택', type: 'single', minSelections: 1, maxSelections: 1, options: [
      { id: 'coffee', label: '커피', symbol: '☕' }, { id: 'tea', label: '차', symbol: '🍵' }, { id: 'fruit', label: '과일 음료', symbol: '🍋' }, { id: 'dessert', label: '디저트', symbol: '🍰' }, { id: 'any', label: '상관없어요', symbol: '✳' },
    ] },
    { id: 'taste', label: '선호하는 맛을\n선택해주세요', description: '최대 2개 선택', type: 'multiple', minSelections: 1, maxSelections: 2, options: [
      { id: 'sweet', label: '달콤한', symbol: '🍯' }, { id: 'nutty', label: '고소한', symbol: '🥜' }, { id: 'fresh', label: '산뜻한', symbol: '🍃' }, { id: 'bitter', label: '쌉싸름한', symbol: '🍫' }, { id: 'any', label: '상관없어요', symbol: '✳' },
    ] },
    { id: 'intensity', label: '맛의 강도를\n선택해주세요', description: '1개 선택', type: 'single', minSelections: 1, maxSelections: 1, options: [
      { id: 'light', label: '은은하게', symbol: '◔' }, { id: 'medium', label: '적당하게', symbol: '◑' }, { id: 'strong', label: '진하게', symbol: '●' }, { id: 'any', label: '상관없어요', symbol: '✳' },
    ] },
    { id: 'caffeine', label: '카페인 선호를\n선택해주세요', description: '1개 선택', type: 'single', minSelections: 1, maxSelections: 1, options: [
      { id: 'okay', label: '괜찮아요', symbol: '☕' }, { id: 'decaf', label: '디카페인을 선호해요', symbol: '🌙' }, { id: 'none', label: '카페인 없는 메뉴를 선호해요', symbol: '🌿' },
    ] },
    { id: 'budget', label: '가격대를\n선택해주세요', description: '1개 선택', type: 'single', minSelections: 1, maxSelections: 1, options: [
      { id: 'low', label: '5,000원 이하', symbol: '₩' }, { id: 'mid', label: '5,000원 초과 ~ 8,000원', symbol: '₩' }, { id: 'high', label: '8,000원 초과', symbol: '₩' }, { id: 'any', label: '상관없어요', symbol: '✳' },
    ] },
  ],
}

export type DemoCafe = {
  id: string
  name: string
  roadAddress: string
  location: { latitude: number; longitude: number }
}

// 가상 카페입니다. 좌표는 행궁동 내 시연 지점이며 실제 매장의 위치를 뜻하지 않습니다.
export const mockCafes: DemoCafe[] = [
  { id: 'cafe-01', name: '모퉁이 커피', roadAddress: '경기도 수원시 팔달구 신풍로 일대 · 시연 지점', location: { latitude: 37.2851, longitude: 127.0139 } },
  { id: 'cafe-02', name: '느린 오후', roadAddress: '경기도 수원시 팔달구 화서문로 일대 · 시연 지점', location: { latitude: 37.2866, longitude: 127.0120 } },
  { id: 'cafe-03', name: '초록의 시간', roadAddress: '경기도 수원시 팔달구 정조로 일대 · 시연 지점', location: { latitude: 37.2878, longitude: 127.0156 } },
  { id: 'cafe-04', name: '오월의 식탁', roadAddress: '경기도 수원시 팔달구 신풍로 일대 · 시연 지점', location: { latitude: 37.2841, longitude: 127.0149 } },
  { id: 'cafe-05', name: '온유', roadAddress: '경기도 수원시 팔달구 화서문로 일대 · 시연 지점', location: { latitude: 37.2859, longitude: 127.0111 } },
  { id: 'cafe-06', name: '오렌지 필름', roadAddress: '경기도 수원시 팔달구 정조로 일대 · 시연 지점', location: { latitude: 37.2888, longitude: 127.0142 } },
  { id: 'cafe-07', name: '담장 너머', roadAddress: '경기도 수원시 팔달구 신풍로 일대 · 시연 지점', location: { latitude: 37.2838, longitude: 127.0128 } },
  { id: 'cafe-08', name: '작은 숲', roadAddress: '경기도 수원시 팔달구 정조로 일대 · 시연 지점', location: { latitude: 37.2860, longitude: 127.0164 } },
]

export type DemoMenu = {
  id: string
  cafeId: string
  name: string
  description: string
  priceKrw: number | null
  isDemo: true
  availability: 'UNKNOWN' | 'SEASON_ENDED'
  imageUrl: string
  imageAlt: string
  demoTags: string[]
}

export const mockMenus: DemoMenu[] = [
  { id: 'menu-01', cafeId: 'cafe-01', name: '솔티드 크림 라테', description: '에스프레소와 우유, 솔티드 크림.', priceKrw: 6500, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/latte.svg', imageAlt: '크림 라테 일러스트', demoTags: ['달콤한', '고소한'] },
  { id: 'menu-02', cafeId: 'cafe-02', name: '말차 클라우드', description: '말차와 우유 크림.', priceKrw: 7000, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/matcha.svg', imageAlt: '말차 음료 일러스트', demoTags: ['쌉싸름한', '부드러운'] },
  { id: 'menu-03', cafeId: 'cafe-03', name: '레몬 바질 소다', description: '레몬과 바질을 넣은 탄산음료.', priceKrw: 6800, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/lemon.svg', imageAlt: '레몬 소다 일러스트', demoTags: ['산뜻한', '상큼한'] },
  { id: 'menu-04', cafeId: 'cafe-04', name: '딸기 치즈 케이크', description: '딸기를 올린 치즈 케이크.', priceKrw: 8500, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/cake.svg', imageAlt: '딸기 케이크 일러스트', demoTags: ['달콤한', '진한'] },
  { id: 'menu-05', cafeId: 'cafe-05', name: '오트 플랫화이트', description: '에스프레소와 오트 음료.', priceKrw: 5800, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/latte.svg', imageAlt: '오트 커피 일러스트', demoTags: ['고소한', '진한'] },
  { id: 'menu-06', cafeId: 'cafe-06', name: '오렌지 선셋', description: '오렌지 탄산음료.', priceKrw: 6200, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/orange.svg', imageAlt: '오렌지 음료 일러스트', demoTags: ['산뜻한', '달콤한'] },
  { id: 'menu-07', cafeId: 'cafe-07', name: '얼그레이 밀크티', description: '얼그레이 홍차와 우유.', priceKrw: null, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/tea.svg', imageAlt: '밀크티 일러스트', demoTags: ['은은한', '부드러운'] },
  { id: 'menu-08', cafeId: 'cafe-08', name: '허니 캐모마일', description: '캐모마일 차와 꿀.', priceKrw: 5000, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/tea.svg', imageAlt: '따뜻한 차 일러스트', demoTags: ['은은한', '달콤한'] },
  { id: 'menu-09', cafeId: 'cafe-01', name: '디카페인 아메리카노', description: '디카페인 에스프레소와 물.', priceKrw: 4800, isDemo: true, availability: 'UNKNOWN', imageUrl: '/illustrations/coffee.svg', imageAlt: '아이스 커피 일러스트', demoTags: ['고소한', '깔끔한'] },
  { id: 'menu-10', cafeId: 'cafe-04', name: '시즌 딸기 소다', description: '딸기를 넣은 시즌 탄산음료.', priceKrw: 6500, isDemo: true, availability: 'SEASON_ENDED', imageUrl: '/illustrations/orange.svg', imageAlt: '시즌 음료 일러스트', demoTags: ['시즌 종료'] },
]

export const demoMap = {
  imageUrl: '/maps/haenggung.svg',
  bounds: [[37.277, 127.002], [37.298, 127.026]] as [[number, number], [number, number]],
  center: [37.2860, 127.0140] as [number, number],
}
