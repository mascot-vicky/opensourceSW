# Docker Compose에서 외부 카탈로그 사용

관련 작업: [Issue #29](https://github.com/seoJing/opensourceSW/issues/29) · 선행 코드: [PR #23](https://github.com/seoJing/opensourceSW/pull/23), [Issue #24](https://github.com/seoJing/opensourceSW/issues/24)

기본 `docker compose up`은 카탈로그 없이 기존 세 서비스를 실행한다. 실제 카탈로그가 필요한 시연에서만 저장소 루트의 `docker-compose.yml`에 `backend/compose.catalog.yml`을 더한다. 확장 파일은 서비스 수를 늘리지 않고 백엔드에 외부 JSON을 **읽기 전용**으로 연결한다. 카탈로그를 DB로 옮기거나 HTTP API를 추가하지 않는다.

## 준비

1. 팀 내부에서 받은 버전별 묶음을 [전달 절차 PR #28](https://github.com/seoJing/opensourceSW/pull/28)에 따라 풀고 SHA-256, `dataVersion`, 카페·메뉴 수를 확인한다. 검증되지 않은 파일로 실행하지 않는다.
2. `catalog.json`의 절대 경로를 확인한다. 파일은 백엔드 컨테이너의 비루트 사용자도 읽을 수 있어야 한다. 파일·디렉터리 경로를 Git 저장소에 복사하거나 커밋하지 않는다.
3. PR #23의 로더와 Issue #24의 시작 시 로딩 코드가 실행할 브랜치에 있어야 한다. 둘 중 하나가 없으면 카탈로그 기능을 확인할 수 없다.

WSL·Linux에서는 저장소 루트에서 실행한다.

```sh
export CATALOG_FILE="$(realpath /path/to/haenggung-2026-11-18.2/catalog.json)"
test -f "$CATALOG_FILE"
docker compose -f docker-compose.yml -f backend/compose.catalog.yml config --quiet
docker compose -f docker-compose.yml -f backend/compose.catalog.yml up --build -d
```

Windows PowerShell에서는 같은 저장소 루트에서 실행한다.

```powershell
$env:CATALOG_FILE = (Resolve-Path 'C:\path\to\haenggung-2026-11-18.2\catalog.json').Path
docker compose -f docker-compose.yml -f backend/compose.catalog.yml config --quiet
docker compose -f docker-compose.yml -f backend/compose.catalog.yml up --build -d
```

`CATALOG_FILE`을 설정하지 않으면 확장 Compose 설정 자체가 실패한다. 파일이 없거나 JSON이 잘못되면 백엔드가 시작에 실패해야 한다. Compose의 파일 경로는 **호스트 경로**이며, 애플리케이션의 `APP_CATALOG_PATH=/app/data/catalog.json`은 **컨테이너 내부 경로**다. 두 값을 바꿔 쓰지 않는다.

## 실행 확인과 종료

```sh
docker compose -f docker-compose.yml -f backend/compose.catalog.yml ps
docker compose -f docker-compose.yml -f backend/compose.catalog.yml logs backend
curl -fsS http://localhost:3000/api/health
```

세 서비스가 healthy이고 백엔드 로그에 `Catalog loaded: schemaVersion=1, cafes=320, menus=5011`이 있어야 이 버전의 파일 연결을 확인할 수 있다. `GET /api/health`는 프론트에서 백엔드·DB 연결을 확인한다. 현재 카탈로그 조회 HTTP API와 프론트 `/api/v1` 프록시는 별도 작업이므로 health 성공만으로 메뉴 화면 연동을 주장하지 않는다.

재시작 후 같은 로그·health를 확인한다. `postgres_data` 볼륨은 PostgreSQL 파일을 보존하지만 카탈로그가 DB에 적재된 것은 아니다. 파일을 새 버전으로 바꿀 때는 새 버전 폴더를 먼저 검증하고 `CATALOG_FILE`을 변경해 백엔드를 재시작한다. 문제가 생기면 이전 파일 경로로 되돌려 재시작한다.

```sh
docker compose -f docker-compose.yml -f backend/compose.catalog.yml down
```

`down`은 컨테이너를 중지·제거하며 named volume은 유지한다. `down -v`는 사용하지 않는다.

## 확인 범위

로컬에서는 Docker daemon을 사용할 수 없어 확장 설정의 해석만 확인했다. 기본 구성과 확장 구성의 실제 빌드·세 서비스 health·재시작 결과는 Docker가 실행되는 팀 PC에서 검증해야 한다. 데이터를 저장소에 넣지 않았으므로 CI의 기본 Compose smoke test는 실제 카탈로그 파일을 읽지 않는다.
