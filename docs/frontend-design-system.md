# 프론트엔드 SEED 사용 기준

관련 작업: [Issue #34](https://github.com/seoJing/opensourceSW/issues/34)

React·TypeScript·Vite·pnpm 기반 프론트에 SEED를 적용합니다. 현재 적용 화면은 개발 환경 상태 화면이며 로그인·설문·메뉴·카페 화면과 업무 API를 추가하지 않습니다.

## 의존성과 선택 이유

| 패키지 | 고정 버전 | 역할 |
| --- | --- | --- |
| `@seed-design/react` | 3.3.0 | React 컴포넌트·레이아웃·타이포그래피 |
| `@seed-design/css` | 3.3.0 | 공개 토큰과 컴포넌트 스타일 |
| `@seed-design/vite-plugin` | 2.1.1 | Vite 스타일 로딩과 색상 모드 설정 |
| `@seed-design/cli` | 1.7.4 | 스니펫 추가·호환성 확인. 개발 의존성 |
| `@juggle/resize-observer` | 3.4.0 | jsdom에 없는 ResizeObserver의 테스트 전용 polyfill |

직접 CSS로 공통 UI를 구현하는 대안은 컴포넌트 상태·접근성·시각 규칙을 팀이 각각 관리해야 합니다. SEED는 이번 사용자의 선택이며, 공식 React 패키지와 토큰을 함께 사용해 그 관리 범위를 줄입니다. CLI도 고정해 팀원이 같은 명령으로 스니펫을 관리하도록 합니다. Tailwind·Stackflow·Storybook·다른 UI 라이브러리는 이번에 추가하지 않습니다.

React 패키지의 peer 범위는 React·React DOM 18 이상 및 SEED CSS `^3.3.0`입니다. 설치한 Vite 플러그인은 Vite 8과 CSS 3을 지원합니다. 기존 React 19·Vite 8 및 pnpm을 유지합니다. 설치는 `pnpm install --frozen-lockfile`을 사용합니다.

공식 근거:
- [Vite 설치](https://seed-design.io/react/getting-started/installation/vite)
- [CLI 설정](https://seed-design.io/react/getting-started/cli/configuration)
- [CLI 명령](https://seed-design.io/react/getting-started/cli/commands)
- [스니펫 개념](https://seed-design.io/react/components/concepts/snippet)

## 스타일과 화면 기준

- `src/app/main.tsx`에서 `@seed-design/css/base.css`를 전역 CSS보다 먼저 가져옵니다. 컴포넌트 CSS는 공식 패키지·Vite 플러그인의 로딩을 사용합니다.
- 발표 중 OS 설정에 따라 표현이 바뀌지 않도록 `seedDesignPlugin({ colorMode: 'light-only' })`를 사용합니다. 다크 모드는 이번 검증 범위가 아닙니다.
- 텍스트는 `Text`의 `textStyle`, 레이아웃은 `Box` 등의 공개 props를 우선 사용합니다. DOM 의미에 맞게 `as="h1"`, `as="section"` 등을 지정합니다.
- 색상은 역할 토큰(`fg.neutral`, `fg.criticalContrast` 등)을 사용합니다. CSS에서는 설치본에 존재하는 공개 변수(`--seed-color-fg-neutral` 등)를 확인합니다. 간격·모서리도 SEED 토큰을 사용합니다.
- SEED의 토큰 값이나 `node_modules`를 수정하지 않습니다. `vars/component/*` 같은 비공개 내부 스타일에 의존하지 않습니다.
- 화면 최대 너비·반응형 분기처럼 화면 배치에 필요한 값은 화면 CSS로 둘 수 있습니다. 같은 의미의 색상·글자 크기·간격을 임의 값으로 복제하지 않습니다.
- 글꼴은 기존 시스템 폰트 fallback을 유지합니다. Pretendard 파일이나 외부 CDN 폰트를 새로 배포하지 않습니다.

공식 [역할 색상](https://seed-design.io/foundations/color/color-role), [간격](https://seed-design.io/foundations/spacing), [Text](https://seed-design.io/react/components/typography/text) 문서를 기준으로 검토합니다.

## 파일 배치와 공개 진입점

```text
frontend/
├── seed-design.json                  CLI의 React·TSX·스니펫 위치 설정
└── src/
    ├── app/                          앱 진입과 전역 스타일
    ├── pages/status/                 상태 화면·health 조회·사용자 동작 테스트
    └── shared/ui/
        ├── index.ts                  앱 코드에 제공하는 공통 UI 진입점
        └── seed-design/
            ├── LICENSE, NOTICE       upstream 저작권·라이선스
            └── ui/                   공식 CLI로 추가한 스니펫
```

- 앱에서 로컬 공통 UI는 `shared/ui/index.ts`로 가져옵니다. SEED의 공개 레이아웃·타이포그래피는 `@seed-design/react`에서 직접 가져올 수 있습니다.
- CLI의 `path`는 `./src/shared/ui/seed-design`입니다. `seed-design/*` alias는 같은 디렉터리로 연결하며 TypeScript와 Vite 8의 `resolve.tsconfigPaths`가 함께 해석합니다.
- 기존 FSD 의존 방향·파일명·테스트 배치를 유지합니다. `shared/ui`에는 메뉴·카페·로그인·설문 정책이나 API 호출을 넣지 않습니다. 사용하지 않는 레이어와 컴포넌트를 미리 만들지 않습니다.
- 생성된 `seed-design/ui/*`는 스니펫 내부 경로로 유지합니다. 직접 파일명을 바꾸면 CLI 갱신 경로가 달라지므로 하지 않습니다.

## 스니펫 관리

`frontend/`에서 실제 필요한 항목만 추가합니다.

```sh
pnpm exec seed-design add ui:action-button
pnpm exec seed-design compat
```

이번에 사용한 항목은 `ui:action-button`이며 CLI가 의존 스니펫 `ui:loading-indicator`, `ui:progress-circle`도 추가했습니다. 출처는 [공식 Action Button](https://seed-design.io/react/components/action-button), 레지스트리는 `https://seed-design.io/__registry__/react/ui/`입니다. 2026-11-19 CLI 1.7.4로 설치했고 각 파일의 `@file`·`@requires`·출처 주석을 보존합니다. 스니펫 자체는 저장소에 커밋되어 팀원이 다시 다운로드하지 않아도 됩니다.

로컬 변경은 기존 ESLint를 만족하도록 ActionButtonProps·ProgressCircleProps의 빈 interface를 동등한 type alias로 바꾼 두 곳입니다. 린트 규칙을 끄지 않았습니다. LICENSE·NOTICE는 공식 스킬과 동일 upstream 커밋에서 가져왔습니다.

새로 추가하거나 갱신할 때는 다음을 지킵니다.

1. 설치한 패키지와 스니펫의 `@requires`를 확인하고 `compat`를 실행합니다. 경고가 있으면 원인을 해결한 뒤 진행합니다.
2. 기존 스니펫에 대한 `--on-diff overwrite`를 무조건 실행하지 않습니다. 변경 내용을 검토하고 로컬 변경을 보존합니다.
3. 필요한 공개 export만 `shared/ui/index.ts`에 추가합니다. 같은 기능을 하는 래퍼나 병렬 UI 체계를 만들지 않습니다.
4. 스니펫 수정 이유·원본 버전·검증을 이 문서와 PR에 기록합니다. CLI 버전을 고정해도 원격 registry 내용은 바뀔 수 있으므로 갱신 diff를 리뷰합니다.

## 상태·접근성·테스트

- 입력에는 연결된 label, 오류에는 설명을 제공합니다. 아이콘 단독 버튼에는 접근 가능한 이름을 지정합니다.
- 버튼은 실제 button 요소를 사용하고 type을 명시합니다. loading만으로 disabled가 되는 것으로 가정하지 않습니다. 중복 요청을 막아야 하는 동작에는 둘 다 지정합니다.
- 성공·실패를 색상만으로 구분하지 않습니다. 비동기 상태는 문구와 적절한 `aria-live`로 안내하고, 실패를 성공이나 빈 결과로 바꾸지 않습니다.
- 320px 너비, 긴 한국어 문구, 키보드 Tab·Enter와 포커스 표시를 확인합니다. 외부 이미지·폰트가 없어도 기본 화면이 읽혀야 합니다.
- 테스트는 구현 세부 class 대신 사용자가 보는 상태와 복구 동작을 확인합니다. 상태 화면은 health 조회 실패 후 재시도와 확인 중 중복 클릭 방지를 검증합니다.
- jsdom에서 SEED 패키지가 가져오는 CSS를 처리하기 위해 Vite test 설정의 `server.deps.inline`에 React·CSS 패키지를 지정합니다. SEED UI를 가짜 컴포넌트로 대체하지 않습니다. [Vitest 공식 오류 안내](https://vitest.dev/guide/common-errors)
- SEED의 크기 관찰에 필요한 ResizeObserver는 jsdom에 없어 테스트 setup에서 실제 polyfill을 등록합니다. 아무 동작도 하지 않는 가짜 observer 대신 표준 API 구현을 사용하고, 앱 번들에는 가져오지 않습니다. 테스트 간 DOM은 명시적으로 cleanup합니다.

```sh
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

공통 UI·실행 설정 변경은 Issue에서 영향과 검증을 합의하고 `.github/harness-policy.json` 감지 목록을 유지합니다. Docker 전체 연결 확인은 기존 루트 스크립트·CI를 따릅니다.

## AI 스킬과 팀 컨벤션

공식 [seed-design 스킬](../.agents/skills/seed-design/SKILL.md)과 참조 파일을 저장소에 포함합니다. [UPSTREAM.md](../.agents/skills/seed-design/UPSTREAM.md)에 고정 커밋·라이선스·갱신 절차를 기록했습니다. 개인 전역 설치 없이 같은 checkout에서 공유하며 Codex에서는 다음 턴부터 사용합니다.

SEED 관련 작업은 이 스킬을 읽고 `frontend/`를 대상으로 공식 문서·CLI·설치본의 계약을 확인합니다. 현재 패키지와 문서가 다른 경우 실제 호환성을 검토하고, 스니펫을 추측해 작성하지 않습니다.

SEED 자체 개발용 스킬과 Bun·릴리스·changeset·브랜치 규칙은 도입하지 않습니다. 이 저장소의 AGENTS·FSD·pnpm·ESLint·Issue/PR 규칙을 유지하며, 최소 하나의 리뷰를 받은 뒤 병합합니다.
