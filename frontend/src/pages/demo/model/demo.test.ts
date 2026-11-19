import { describe, expect, it } from 'vitest'
import { mockCafes, mockMenus, mockQuestionnaire } from '../mock'
import { directionsUrl, formatPrice, toggleAnswer } from './demo'

describe('시연 설문과 상수 데이터', () => {
  it('복수 선택에서 상관없어요는 배타적이고 선택 해제와 최대 개수를 지킨다', () => {
    const question = mockQuestionnaire.questions[1]
    expect(toggleAnswer(question, ['any'], 'sweet')).toEqual(['sweet'])
    expect(toggleAnswer(question, ['sweet', 'nutty'], 'any')).toEqual(['any'])
    expect(toggleAnswer(question, ['sweet', 'nutty'], 'fresh')).toEqual(['sweet', 'nutty'])
    expect(toggleAnswer(question, ['sweet', 'nutty'], 'sweet')).toEqual(['nutty'])
  })
  it('모든 메뉴는 존재하는 카페를 가리키고 시연 데이터로 표시된다', () => {
    const cafes = new Set(mockCafes.map(cafe => cafe.id))
    expect(cafes.size).toBe(mockCafes.length)
    expect(new Set(mockMenus.map(menu => menu.id)).size).toBe(mockMenus.length)
    for (const menu of mockMenus) {
      expect(cafes.has(menu.cafeId)).toBe(true)
      expect(menu.isDemo).toBe(true)
    }
  })
  it('가격 미확인을 무료와 구분한다', () => {
    expect(formatPrice(null)).toBe('가격 미확인')
    expect(formatPrice(0)).toBe('0원')
    expect(formatPrice(6500)).toBe('6,500원')
  })
  it('외부 지도 URL은 선택한 카페의 목적지 이름과 좌표를 포함한다', () => {
    const cafe = mockCafes[0]
    const kakao = new URL(directionsUrl('kakao', cafe))
    expect(kakao.hostname).toBe('map.kakao.com')
    expect(decodeURIComponent(kakao.pathname)).toContain(`${cafe.location.latitude},${cafe.location.longitude}`)
    const naver = new URL(directionsUrl('naver', cafe))
    expect(naver.hostname).toBe('map.naver.com')
    expect(decodeURIComponent(naver.pathname)).toContain(cafe.name)
    expect(naver.pathname.endsWith('/walk')).toBe(true)
  })
})
