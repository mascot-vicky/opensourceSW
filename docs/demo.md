# 중간 발표 시연 화면

관련 작업: [Issue #36](https://github.com/seoJing/opensourceSW/issues/36)

## 실행과 화면

루트에서 `sh scripts/start.sh`(Windows는 `./scripts/start.ps1`)를 실행하고 `http://localhost:3000/`에 접속합니다. 기존 연결 확인 화면은 `/status`, Nginx health는 `/api/health`에서 유지합니다. Vite 단독 개발은 frontend에서 `pnpm dev`로 실행하며 업무 API 없이 시연할 수 있습니다.

화면 이름은 **카페 지도**입니다. 화면의 표지·문구와 벡터 일러스트는 이 작업에서 작성했습니다. 색상·타이포그래피·컴포넌트는 SEED를 사용하고, 한 문항씩 진행하는 설문과 짧은 순차 슬라이드로 화면을 연결합니다.

1. 표지에서 시작하기 → 로그인. 원하는 비어 있지 않은 아이디·비밀번호로 진행합니다. 실제 인증이 아니며 비밀번호를 저장하거나 전송하지 않습니다. 진입 후 입력값은 비웁니다.
2. 첫 방문 안내 → 설문 5문항. 단일·복수 선택, 상관없어요 배타 선택, 이전 문항 복원과 진행 버튼을 제공합니다.
3. 지도 메인. 로컬 행궁동 지도 위에 가상 카페 8곳을 표시합니다. 드래그·확대 축소·카페명/메뉴명 검색·마이페이지를 제공합니다.
4. 하단 원형 버튼 → 지도 위 메뉴 카드 탐색. 시즌 종료 1개를 제외한 9개 카드의 가로 스크롤·이전/다음과 카페 핀 강조가 연동됩니다. 별도 탐색 완료 화면은 없습니다.
5. 지도 핀 또는 메뉴 선택 → 공통 SEED 바텀시트. 카페·주소·메뉴·가격·판매 미확인/시즌 종료 상태를 보여주며 주소 복사와 네이버·카카오 목적지 링크를 제공합니다. 닫으면 직전 카드·지도 상태를 유지합니다.
6. 마이페이지 → 설문 다시하기. 저장된 로컬 답변으로 시작하고 취소하면 이전 답변을 유지합니다. 로그아웃은 답변·탐색 상태를 지우고 표지로 돌아갑니다. 새로고침도 처음부터 시작합니다.

설문 입력은 시연 흐름을 보여주기 위한 것이며 메뉴 순서·취향 점수·추천 엔진을 구현하지 않습니다. 카드 넘김은 선호도 학습이나 서버 탐색 이력 저장이 아닙니다.

## 상수 mock과 코드 배치

`frontend/src/pages/demo/mock/index.ts`의 `mockUser`, `mockQuestionnaire`, `mockCafes`, `mockMenus`, `demoMap`을 직접 import합니다. MSW·mock 서버·가짜 HTTP 계층·지연 응답·계정 저장소·localStorage는 추가하지 않습니다. React useState로 화면 진행과 답변·선택 상태를 관리합니다.

- `mock/`: 시연용 객체와 화면에 필요한 로컬 타입. id/cafeId/name/description/priceKrw/location 등 백엔드의 필드명을 참고한 표시용 부분집합입니다. 정확한 서버 DTO나 새 API 계약이 아닙니다.
- `model/demo.ts`: 선택지 토글, 가격 표시, 외부 목적지 링크 구성.
- `ui/`: 진입·설문, 지도·카드, 공통 카페 시트와 페이지 조립. 외부에서 사용할 공개 진입점은 `pages/demo/index.ts`입니다.
- `shared/ui/`: 공식 SEED 스니펫과 공개 export. 카페 데이터·시연 정책은 이 레이어에 넣지 않습니다.
- `public/maps/`: OSM 기반 정적 SVG와 출처·범위·투영 기록.
- `public/illustrations/`: 직접 작성한 메뉴와 표지용 추상 지도 벡터 일러스트. 메뉴 사진이나 실제 제품 이미지가 아닙니다.

카페명·도로 일대·메뉴·맛 태그는 **가상 시연 자료**입니다. 화면 시트에서 이를 안내합니다. `imageUrl`, `imageAlt`는 로컬 표현 필드이고 `demoTags`는 확인된 실제 맛·원재료·카페인 속성이 아닙니다. 가격 null을 0원으로 표시하지 않으며 현재 판매 여부를 보장하지 않습니다. 실제 백엔드 객체·DB·로그인·설문·추천 API를 변경하지 않습니다.

## 지도와 현재 위치

[지도 출처 기록](../frontend/public/maps/README.md)의 EPSG:3857 SVG를 Leaflet ImageOverlay에 연결합니다. API 키와 런타임 지도 타일 요청이 없습니다. 좌표와 이미지 범위가 일치해야 핀이 맞습니다. 지도 출처 링크는 화면 하단에 표시합니다.

현재 위치 버튼을 눌렀을 때만 브라우저 Geolocation을 요청합니다. 좌표는 현재 화면의 마커에만 쓰며 서버·외부 지도 서비스로 보내거나 저장하지 않습니다. 권한 거부·조회 실패·범위 밖이면 안내하고 이전 위치 마커를 제거합니다. 지도 탐색은 계속 가능합니다. 재설문·로그아웃으로 지도 화면을 떠난 뒤 도착한 위치 응답은 적용하지 않습니다.

외부 지도 버튼은 가상 카페의 **시연 지점**으로 연결합니다. 카카오는 [공식 지도 URL](https://apis.map.kakao.com/web/guide/#routeurl)의 목적지 위경도 링크를 사용합니다. 네이버는 Web Mercator 좌표를 사용하는 현재 웹 길찾기 링크로 연결합니다. 웹 링크는 버전이 보장된 API가 아니므로 발표 전 목적지 매핑을 재확인해야 합니다. 기기별 외부 앱 설치·최종 출발지·경로 계산은 외부 서비스가 담당합니다. 이 작업은 길찾기 API를 구현하거나 호출하지 않습니다.

## 의존성과 스니펫 변경

- `leaflet@1.9.4`(BSD-2-Clause), `@types/leaflet@1.9.21`(개발 타입): 공식 안정판으로 정적 지도 위 지리 좌표·마커·제스처를 관리합니다. 직접 CSS 백분율로 핀 좌표와 이동을 구현하는 대안보다 투영·접근성과 상태 유지를 일관되게 처리합니다. react-leaflet은 추가하지 않습니다. [배포용 라이선스 사본](../frontend/public/licenses/leaflet.txt)을 포함합니다. [공식 레퍼런스](https://leafletjs.com/reference-1.9.4.html#imageoverlay)
- `@karrotmarket/react-monochrome-icon@1.35.0`(Apache-2.0): SEED BottomSheet·TextField 스니펫의 공식 의존성이며 앱 아이콘에도 사용합니다. 별도의 아이콘 라이브러리를 추가하는 대안 대신 같은 체계를 사용합니다. [라이선스](../frontend/public/licenses/seed-icon.txt)와 [NOTICE](../frontend/public/licenses/seed-icon-NOTICE.txt)를 보존합니다.
- CLI 1.7.4로 `ui:bottom-sheet`, `ui:text-field`를 추가했습니다. 출처·요구 버전 주석을 보존합니다. 기존 ESLint에 맞춰 빈 interface를 type alias로 바꾸고, 브라우저용 Vite 프로젝트에서 Node 타입을 추가하지 않도록 개발 경고 조건을 `import.meta.env.DEV`로 바꿨습니다.
- 별도 모션 라이브러리 없이 CSS transition·keyframes를 사용합니다. reduced-motion 설정에서는 이동·지연·반복 모션을 줄입니다.
- 지도 코드는 지도 화면 진입 시 lazy import하여 초기 표지 번들에서 분리합니다. 외부 폰트·이미지 CDN·OSM 타일에 의존하지 않습니다.

## 검증

frontend에서 `pnpm install --frozen-lockfile`, `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build`, `pnpm exec seed-design compat`를 사용합니다. 루트의 하네스 테스트·Repository Guard·공백 검사를 함께 실행합니다.

사용자 동작 테스트는 빈 로그인·비밀번호 초기화·필수 선택·이전 답변 유지·복수 선택 제한·재설문 취소·mock 참조·가격 null·외부 링크 매핑을 확인합니다. 실제 Leaflet·SEED 시트·위치 권한은 Chrome 브라우저에서 별도 검증합니다. 지도 라이브러리나 SEED를 가짜 컴포넌트로 바꿔 통과시키지 않습니다.

시연 검수: 320px·390px·데스크톱에서 전체 흐름, 키보드와 모달 닫기, 지도 이동·확대/축소, 카드/핀 선택·복귀, 빈 검색, 현재 위치 성공·거부·범위 밖, reduced-motion을 확인합니다. Docker는 기본 세 서비스 healthy와 프론트·health 응답을 확인하며 외부 지도 앱의 실제 경로 안내까지 자동화한 것으로 주장하지 않습니다.
