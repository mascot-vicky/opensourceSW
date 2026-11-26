import '@testing-library/jest-dom/vitest'
import { ResizeObserver } from '@juggle/resize-observer'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// jsdom에는 레이아웃 관찰 API가 없어 실제 polyfill을 테스트 환경에만 제공합니다.
globalThis.ResizeObserver = ResizeObserver

afterEach(cleanup)
