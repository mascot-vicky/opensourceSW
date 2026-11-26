import { useEffect, useState } from 'react'
import { Box, Text } from '@seed-design/react'

import { ActionButton } from '../../../shared/ui'
import { getHealth } from '../api/getHealth'

const checkingStatus = {
  className: 'checking',
  label: '확인 중',
  message: '백엔드 상태를 확인하고 있습니다.',
}

export default function StatusPage() {
  const [backendStatus, setBackendStatus] = useState(checkingStatus)
  const [checkVersion, setCheckVersion] = useState(0)

  useEffect(() => {
    let isMounted = true

    async function checkBackend() {
      try {
        const status = await getHealth()
        if (isMounted) {
          const isUp = status === 'UP'
          setBackendStatus({
            className: isUp ? 'up' : 'down',
            label: isUp ? '연결됨 (UP)' : `응답 상태: ${status ?? '알 수 없음'}`,
            message: isUp
              ? 'Spring Boot와 PostgreSQL 연결이 정상입니다.'
              : '백엔드 또는 데이터베이스 상태를 확인해 주세요.',
          })
        }
      } catch {
        if (isMounted) {
          setBackendStatus({
            className: 'down',
            label: '연결 실패',
            message: '백엔드 컨테이너와 health check 상태를 확인해 주세요.',
          })
        }
      }
    }

    checkBackend()

    return () => {
      isMounted = false
    }
  }, [checkVersion])

  function recheckBackend() {
    setBackendStatus(checkingStatus)
    setCheckVersion(version => version + 1)
  }

  return (
    <main className="page-shell">
      <Box as="section" className="status-card" aria-labelledby="project-title" bg="bg.layerDefault" borderRadius="r4" borderWidth="1" borderColor="stroke.neutralMuted">
        <Text as="p" color="fg.brand" textStyle="t3Bold">초기 개발 환경</Text>
        <Text as="h1" id="project-title" className="project-title" textStyle="t9Bold">수원 랜덤 여행 코스</Text>
        <Text as="p" color="fg.neutralMuted" textStyle="t5Regular" className="intro">
          팀 개발을 시작하기 위한 빈 프로젝트가 실행 중입니다. 여행 기능은 아직 구현하지 않았습니다.
        </Text>

        <dl className="status-list">
          <div>
            <dt>프론트엔드</dt>
            <dd className="status-value up">실행 중</dd>
          </div>
          <div>
            <dt>백엔드 연결</dt>
            <dd className={`status-value ${backendStatus.className}`} aria-live="polite">
              <strong>{backendStatus.label}</strong>
              <span>{backendStatus.message}</span>
            </dd>
          </div>
        </dl>

        <ActionButton
          type="button"
          aria-label="연결 다시 확인"
          variant="neutralOutline"
          size="medium"
          loading={backendStatus.className === 'checking'}
          disabled={backendStatus.className === 'checking'}
          onClick={recheckBackend}
        >
          연결 다시 확인
        </ActionButton>

        <Text as="p" color="fg.neutralSubtle" textStyle="t4Regular" className="notice">POI, 추천, 지도 등 실제 여행 기능은 팀 협업을 통해 이후 추가할 예정입니다.</Text>
      </Box>
    </main>
  )
}
