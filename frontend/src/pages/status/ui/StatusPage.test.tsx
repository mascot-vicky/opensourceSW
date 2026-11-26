import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import StatusPage from './StatusPage'

describe('StatusPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('초기 개발 환경과 백엔드 상태 영역을 렌더링한다', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))

    render(<StatusPage />)

    expect(screen.getByRole('heading', { name: '수원 랜덤 여행 코스' })).toBeInTheDocument()
    expect(screen.getByText('초기 개발 환경')).toBeInTheDocument()
    expect(screen.getByText('백엔드 연결')).toBeInTheDocument()
    expect(screen.getByText('확인 중')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '연결 다시 확인' })).toBeDisabled()
  })

  it('Actuator가 UP이면 정상 연결 상태를 표시한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ status: 'UP' }),
      }),
    )

    render(<StatusPage />)

    expect(await screen.findByText('연결됨 (UP)')).toBeInTheDocument()
    expect(screen.getByText('Spring Boot와 PostgreSQL 연결이 정상입니다.')).toBeInTheDocument()
  })

  it('연결 실패 후 다시 확인하면 정상 상태로 복구하고 확인 중 중복 요청을 막는다', async () => {
    let resolveRetry!: (response: { ok: boolean; json: () => Promise<{ status: string }> }) => void
    const fetchHealth = vi.fn()
      .mockRejectedValueOnce(new Error('network unavailable'))
      .mockImplementationOnce(() => new Promise(resolve => { resolveRetry = resolve }))
    vi.stubGlobal('fetch', fetchHealth)

    render(<StatusPage />)

    expect(await screen.findByText('연결 실패')).toBeInTheDocument()
    const retry = screen.getByRole('button', { name: '연결 다시 확인' })
    fireEvent.click(retry)
    expect(retry).toBeDisabled()
    fireEvent.click(retry)
    expect(fetchHealth).toHaveBeenCalledTimes(2)

    resolveRetry({ ok: true, json: async () => ({ status: 'UP' }) })
    expect(await screen.findByText('연결됨 (UP)')).toBeInTheDocument()
    expect(retry).toBeEnabled()
  })

  it('정상 HTTP 응답이라도 UP이 아니면 연결 성공으로 표시하지 않는다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ status: 'DOWN' }),
    }))

    render(<StatusPage />)

    expect(await screen.findByText('응답 상태: DOWN')).toBeInTheDocument()
    expect(screen.queryByText('연결됨 (UP)')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '연결 다시 확인' })).toBeEnabled()
  })
})
