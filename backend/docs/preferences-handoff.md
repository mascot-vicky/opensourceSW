# 로그인과 설문 연동

프론트에서 연동할 순서는 로그인 → 설문 정의·저장값 조회 → 답변 제출 → 추천 카드 조회입니다. PR #26에서 문항 조회, 계정별 답변 저장·복원, 추천용 선호값 변환을 함께 제공합니다. 현재 시연 화면은 로컬 로그인과 고정 5문항을 사용합니다. 연동용 v0.1.0은 6문항 초안입니다.

- [OpenAPI 3.0 명세](preferences-openapi.yaml)에서 네 경로의 요청·응답·오류를 확인할 수 있습니다.
- [문항 JSON](examples/questionnaire-v0.1.0.json)은 아래 실행 설정에서 서버가 실제로 읽는 파일입니다. 화면 개발용 mock에도 같은 파일을 사용할 수 있습니다.

## 팀 PC에서 실행

Docker Compose를 사용할 수 있는 터미널에서 별도 폴더에 브랜치를 받습니다. 아래 명령은 그 폴더의 저장소 루트에서 실행합니다. 백엔드는 기본 8080 포트를 사용합니다.

```sh
git clone --branch feature/backend/20-questionnaire-read --single-branch https://github.com/mascot-vicky/opensourceSW.git opensw-fe-integration
cd opensw-fe-integration
docker compose -p suwon-fe build backend
docker compose -p suwon-fe run --rm --no-deps --entrypoint java backend "-Dloader.main=com.suwontrip.app.auth.support.PasswordHashTool" -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

마지막 명령에서 이 PC의 시연 비밀번호를 정합니다. 출력된 BCrypt 해시를 같은 터미널의 환경변수에 넣습니다.

Windows PowerShell:

```powershell
$env:DEMO_PASSWORD_HASH = Read-Host '출력된 BCrypt 해시를 붙여 넣으세요'
```

Bash:

```bash
read -rs -p '출력된 BCrypt 해시를 붙여 넣으세요: ' DEMO_PASSWORD_HASH
echo
export DEMO_PASSWORD_HASH
```

```sh
docker compose -p suwon-fe -f docker-compose.yml -f backend/compose.preferences.yml up -d --wait backend
```

브라우저에서 `http://localhost:8080/api/v1/auth/session`을 열면 처음에는 `authenticated: false`와 CSRF 토큰이 보입니다. 로그인 ID는 `fe-demo`이며 비밀번호는 위에서 직접 입력한 값입니다. 종료할 때는 아래 명령을 사용합니다. 답변은 `suwon-fe_preferences_data` 볼륨에 보존됩니다.

```sh
docker compose -p suwon-fe -f docker-compose.yml -f backend/compose.preferences.yml down
```

## 카탈로그와 같은 DB에서 실행

설문 단독 시연의 기본 볼륨은 `suwon-fe_preferences_data`입니다. 팀 프로젝트의 기존 카탈로그 DB를 이어서 사용할 때는 `SUWON_DB_VOLUME`에 `suwon_trip_postgres_data`를 지정합니다. 기존 설문 답변을 보존하면서 카탈로그를 추가할 때는 `suwon-fe_preferences_data`를 지정합니다. 두 DB를 동시에 같은 볼륨에 연결하지 않도록, 실행 중인 기존 프로젝트는 일반 `down`으로 종료합니다.

Bash에서는 `export SUWON_DB_VOLUME=suwon-fe_preferences_data`, PowerShell에서는 `$env:SUWON_DB_VOLUME = 'suwon-fe_preferences_data'`처럼 사용할 볼륨을 선택합니다. 두 PR이 병합된 체크아웃에서 다음 명령을 사용합니다. 같은 터미널의 시연 계정 해시 설정도 필요합니다.

```sh
docker compose -p opensourcesw -f docker-compose.yml -f backend/compose.preferences.yml -f backend/compose.catalog.yml up --build -d --wait
```

선택한 DB에서 카탈로그 입력을 다시 읽어도 같은 버전·해시의 데이터는 중복되지 않습니다. 이후 재실행에도 같은 볼륨 설정을 사용합니다.

## 브라우저 요청 순서

| 시점 | API | 화면에서 처리할 값 |
| --- | --- | --- |
| 최초 진입·새로고침 | GET /api/v1/auth/session | 인증 상태와 CSRF 토큰을 받습니다. 이미 로그인했다면 설문 정의와 저장값을 조회합니다. |
| 로그인 | POST /api/v1/auth/session | `{loginId, password}`와 현재 CSRF 토큰을 보냅니다. 성공 응답의 새 토큰을 사용합니다. |
| 설문 열기 | GET /api/v1/preference-questionnaire 및 GET /api/v1/me/preferences | 두 요청을 병렬로 보낼 수 있습니다. 저장값이 있고 버전이 일치하면 복원합니다. 구버전이면 새 설문을 안내합니다. |
| 완료·재설문 | PUT /api/v1/me/preferences | 현재 정의의 버전과 모든 문항의 선택지 ID 배열을 보냅니다. 200을 받은 뒤 화면을 이동합니다. |
| 로그아웃 | DELETE /api/v1/auth/session | 현재 CSRF 토큰을 보냅니다. 성공 후 메모리에 보관한 이전 사용자 답을 비웁니다. |

브라우저는 상대 경로 `/api/v1/...`와 같은 출처의 세션 쿠키를 사용합니다. 상태 변경 요청에는 `X-CSRF-TOKEN`, JSON 본문에는 `Content-Type: application/json`이 필요합니다. 비밀번호·쿠키·토큰을 소스나 공유 캡처에 넣지 않습니다.

**프론트팀 연결 지점** — 현재 Vite 개발 서버에는 기능 API 프록시가 없고, Compose의 Nginx는 `/api/health`만 전달합니다. Vite에서는 `/api/v1`을 호스트의 `http://localhost:8080`으로, Nginx에서는 `/api/v1/`을 Compose 네트워크의 `http://backend:8080`으로 경로를 보존해 전달해야 합니다. 프론트 컨테이너는 백엔드와 같은 Compose 네트워크를 사용해야 합니다. 연결 확인은 **프론트 주소**의 `GET /api/v1/auth/session`이 JSON을 반환하는지로 합니다. HTTP 200에 HTML이 오면 SPA 첫 화면으로 라우팅된 상태입니다.

## 설문 화면에서 지킬 규칙

- 문항 수·문구·선택 시 허용 개수는 조회 응답에서 가져옵니다. 문항 ID와 선택지 ID를 한 쌍으로 취급합니다. Q1의 A와 Q4의 A는 다른 답입니다. 화면 장식용 심볼·설명은 응답에 없습니다.
- v0.1.0에서 Q1=A(음료)일 때만 Q4를 표시합니다. Q1이 바뀌어 Q4가 숨겨지면 이전 선택을 지우고 `{"questionId":"Q4","optionIds":[]}`로 제출합니다. 이 표시 조건은 현재 조회 응답에 별도 필드로 제공되지 않습니다.
- 전체 정의 순서로 모든 문항을 보냅니다. 미응답은 빈 배열, ‘상관없음’을 직접 골랐다면 해당 선택지 ID를 보냅니다. 저장 API에는 `userId`나 계산한 속성값을 보내지 않습니다.
- 401이면 로그인 상태를 확인합니다. 403이면 세션과 CSRF 값을 갱신합니다. 409이면 정의를 다시 받아 재설문을 안내합니다. 422이면 입력을 유지한 채 `errors[].questionId`로 오류를 표시합니다.
- 저장 성공은 사용자 답의 보존까지 의미합니다. 추천 카드·지도 핀 API와 메뉴 순위는 후속 범위입니다. 프론트에서 카드·핀에 필요한 필드나 빈 결과 화면 요구가 생기면 Issue #20에서 후속 계약을 요청해 주세요.

## 추천 카드와 매장 표시

같은 출처에서 로그인 세션과 현재 `X-CSRF-TOKEN`을 사용해
`POST /api/v1/menu-recommendations`에 `{"excludedMenuIds":[]}`를 보냅니다.
`200`이면 `menu`를 먼저 확인합니다. null이면 빈 상태를 표시합니다.
카드를 넘길 때 표시한 `menu.id`를 중복 없이 제외 목록에 추가합니다(최대 100개).
카드 선택 시 `menu.cafe`의 매장명·도로명 주소·좌표·길찾기 링크를 바로 사용합니다.
`menu.cafe.id`와 `menu.cafeId`는 같습니다. 좌표 또는 링크가 null이면 해당 기능을 숨기고
주소 복사를 제공합니다. 이미지가 null이면 기본 이미지를 사용합니다.

`401`은 로그인, `403`은 세션·CSRF 갱신, `409 PREFERENCES_REQUIRED`는 설문 재제출로 연결합니다.
`500/503`은 현재 카드를 보존하고 재시도를 안내합니다. #26 단독에서는 설문 저장까지 가능하고,
추천의 정상 응답은 #31과 활성 카탈로그가 필요합니다. 두 PR의 통합본에서 위의 두 Compose
확장 파일을 함께 사용하세요. 현재 고정본은 판매 확인 후보가 0건이므로 정상 응답은 `{"menu":null}`입니다.
