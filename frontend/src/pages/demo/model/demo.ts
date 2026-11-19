import type { DemoCafe, Question } from '../mock'

export type Answers = Record<string, string[]>

export function toggleAnswer(question: Question, current: string[], optionId: string) {
  if (question.type === 'single' || optionId === 'any') return [optionId]
  const selected = current.filter(id => id !== 'any')
  if (selected.includes(optionId)) return selected.filter(id => id !== optionId)
  return selected.length < question.maxSelections ? [...selected, optionId] : selected
}

export function formatPrice(price: number | null) {
  return price === null ? '가격 미확인' : `${price.toLocaleString('ko-KR')}원`
}

export function directionsUrl(provider: 'naver' | 'kakao', cafe: DemoCafe) {
  const { latitude, longitude } = cafe.location
  const destination = encodeURIComponent(`${cafe.name} 시연 위치`)
  if (provider === 'kakao') return `https://map.kakao.com/link/to/${destination},${latitude},${longitude}`
  const x = longitude * Math.PI / 180 * 6378137
  const y = Math.log(Math.tan(Math.PI / 4 + latitude * Math.PI / 360)) * 6378137
  return `https://map.naver.com/p/directions/-/${x.toFixed(4)},${y.toFixed(4)},${destination},,ADDRESS_POI/-/walk`
}
