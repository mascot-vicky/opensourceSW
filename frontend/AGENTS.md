# 프론트엔드 작업 규칙

- 루트 작업 규칙과 공동 작업 가이드를 따릅니다. 공통 CI, 통합 실행, 배포 설정 변경은 루트 정책에 따라 Issue에서 합의합니다.
- FSD 의존 방향은 app → pages → widgets → features → entities → shared입니다. 슬라이스는 하위 레이어 슬라이스의 공개 진입점인 index.ts를 통해 가져옵니다. 같은 레이어의 다른 슬라이스를 직접 가져오지 않습니다. 슬라이스 내부는 상대 경로를 사용합니다.
- 레이어와 세그먼트는 첫 실제 코드가 생길 때 추가합니다. 컴포넌트는 PascalCase, API 함수 파일은 camelCase, 슬라이스 폴더는 kebab-case로 작성합니다.
- 화면 테스트는 파일 곁에 두고 사용자에게 보이는 동작을 검증합니다. 백엔드 계약은 팀에서 합의한 값을 사용합니다.
- 변경 후 pnpm lint, pnpm typecheck, pnpm test, pnpm build를 실행하고 결과를 인수인계합니다.
- SEED 관련 구현·진단·갱신 전에는 [공식 seed-design 스킬](../.agents/skills/seed-design/SKILL.md)과 [프로젝트 사용 기준](../docs/frontend-design-system.md)을 읽습니다. 대상은 React 앱인 frontend이며 SEED 자체 저장소용 스킬·Bun·릴리스 규칙은 적용하지 않습니다.
- SEED의 공개 컴포넌트·토큰을 우선 사용합니다. 로컬 스니펫은 shared/ui/seed-design에 두고 앱에서는 shared/ui/index.ts로 가져옵니다. 도메인 정책·API 호출을 shared/ui에 넣지 않습니다.
- 스니펫의 출처·요구 버전 주석을 보존하고 변경 이유를 사용 기준에 기록합니다. 갱신 전 diff와 pnpm exec seed-design compat 결과를 확인합니다. 린트·테스트를 비활성화하거나 원본을 무조건 덮어쓰지 않습니다.
- 로딩·오류·빈 결과를 구분하고, 중복 요청 방지·키보드 조작·입력 label·포커스·320px 너비를 검증합니다. 공통 UI·SEED 설정 변경은 루트 공통 설정 합의 절차를 따릅니다.
