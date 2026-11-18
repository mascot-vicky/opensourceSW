# Docker Compose에서 외부 카탈로그 사용

관련 작업: [Issue #29](https://github.com/seoJing/opensourceSW/issues/29) · 선행 코드: [PR #23](https://github.com/seoJing/opensourceSW/pull/23), [Issue #24](https://github.com/seoJing/opensourceSW/issues/24)

기본 `docker compose up`은 카탈로그 없이 기존 세 서비스를 실행한다. 실제 카탈로그가 필요한 시연에서만 저장소 루트의 `docker-compose.yml`에 `backend/compose.catalog.yml`을 더한다. 확장 파일은 서비스 수를 늘리지 않고 백엔드에 외부 JSON을 **읽기 전용**으로 연결한다. 카탈로그를 DB로 옮기거나 HTTP API를 추가하지 않는다.

실행자는 자신의 PC에서 파일 경로를 지정한다. 특정 사용자 계정, 공유 서비스나 원격 PC 접근을 전제로 하지 않는다. 이 문서의 목적은 다른 사용자가 실행할 수 있는 설정·절차를 제공하는 것이다.

## 준비

1. Docker Engine과 Compose v2 또는 Docker Desktop을 준비하고 `docker compose version`, `docker info`로 확인한다. 저장소 루트에서 실행하며 3000·8080 포트를 사용할 수 있어야 한다. 호스트 Java·Maven 설치는 필요하지 않다.
2. 버전별 묶음을 [전달 절차 PR #28](https://github.com/seoJing/opensourceSW/pull/28)에 따라 풀고 SHA-256, `dataVersion`, 카페·메뉴 수를 확인한다. 전달 매체는 실행자가 선택한다.
3. `catalog.json`의 절대 경로를 확인한다. 파일은 백엔드 컨테이너의 비루트 사용자도 읽을 수 있어야 한다. 파일·개인 경로는 Git에 커밋하지 않는다.
4. PR #23의 로더와 Issue #24의 시작 시 로딩 코드가 실행할 브랜치에 있어야 한다. 둘 중 하나가 없으면 카탈로그 기능을 확인할 수 없다.

WSL·Linux·macOS 셸에서는 저장소 루트에서 실행한다. 예시 절대 경로를 자신의 파일 위치로 바꾸고, 공백이 있는 경로도 따옴표 안에 넣는다.

```sh
export CATALOG_FILE="/absolute/path/haenggung-2026-11-18.2/catalog.json"
test -f "$CATALOG_FILE" && test -r "$CATALOG_FILE" &&
  docker compose -f docker-compose.yml -f backend/compose.catalog.yml config --quiet &&
  docker compose -f docker-compose.yml -f backend/compose.catalog.yml up --build -d
```

Windows PowerShell에서는 같은 저장소 루트에서 실행한다.

```powershell
$catalogInput = Get-Item -LiteralPath 'C:\path\to\haenggung-2026-11-18.2\catalog.json' -ErrorAction Stop
if ($catalogInput.PSIsContainer) { throw 'catalog.json 파일을 지정하세요.' }
$env:CATALOG_FILE = $catalogInput.FullName
docker compose -f docker-compose.yml -f backend/compose.catalog.yml config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Compose 설정 검사가 실패했습니다.' }
docker compose -f docker-compose.yml -f backend/compose.catalog.yml up --build -d
```

`CATALOG_FILE`을 설정하지 않으면 확장 Compose 설정 자체가 실패한다. 파일이 없거나 JSON이 잘못되면 백엔드가 시작에 실패해야 한다. Compose의 파일 경로는 **호스트 경로**이며, 애플리케이션의 `APP_CATALOG_PATH=/app/data/catalog.json`은 **컨테이너 내부 경로**다. 두 값을 바꿔 쓰지 않는다.

WSL에서 실행할 때는 WSL 경로를 사용하고 Docker Desktop의 해당 배포판 통합을 활성화한다. Windows PowerShell에서는 Windows 경로를 사용한다. Docker Desktop의 파일 공유·읽기 권한도 해당 경로를 허용해야 한다.

## 실행 확인과 종료

```sh
docker compose -f docker-compose.yml -f backend/compose.catalog.yml ps
docker compose -f docker-compose.yml -f backend/compose.catalog.yml logs backend
curl -fsS http://localhost:3000/api/health
```

Windows PowerShell의 health 조회는 `Invoke-RestMethod http://localhost:3000/api/health`를 사용한다.

세 서비스가 healthy이고 백엔드 로그에 `Catalog loaded: schemaVersion=1, cafes=320, menus=5011`이 있어야 이 버전의 파일 연결을 확인할 수 있다. `GET /api/health`는 프론트에서 백엔드·DB 연결을 확인한다. 현재 카탈로그 조회 HTTP API와 프론트 `/api/v1` 프록시는 별도 작업이므로 health 성공만으로 메뉴 화면 연동을 주장하지 않는다.

로그의 건수는 전달받은 `manifest.json`과 대조한다. 위의 320곳·5,011건은 `haenggung-2026-11-18.2`의 예시이며 코드에 고정된 수량이 아니다. `postgres_data` 볼륨은 PostgreSQL 파일을 보존하지만 카탈로그가 DB에 적재된 것은 아니다.

새 버전은 별도 폴더에 검증한 뒤 `CATALOG_FILE`을 새 파일로 바꾸고 아래 명령으로 백엔드를 다시 생성한다. 단순 `restart`는 변경된 호스트 파일 경로를 적용하지 않는다. 이전 버전으로 되돌릴 때도 경로를 바꾼 뒤 같은 명령을 사용한다. 로딩 로그와 health를 다시 확인한다.

```sh
docker compose -f docker-compose.yml -f backend/compose.catalog.yml up -d --no-deps --force-recreate backend
```

종료는 `CATALOG_FILE`이 설정된 셸에서 실행한다.

```sh
docker compose -f docker-compose.yml -f backend/compose.catalog.yml down
```

`down`은 컨테이너를 중지·제거하며 named volume은 유지한다. `down -v`는 사용하지 않는다.

## 확인 범위

로컬에서는 Docker daemon을 사용할 수 없어 확장 설정의 해석만 확인했다. 일반 경로·공백이 있는 경로에서의 읽기 전용 연결과 필수 변수 누락 오류를 검사했다. 실제 카탈로그를 연결한 컨테이너 기동은 미검증이며, CI의 기본 Compose smoke test에도 이 확장 구성은 포함되지 않는다. 다른 PC의 실행은 사용 시 확인할 항목으로 남기며, 특정 PC 접근이나 전달 장소 확정을 이번 구성 작업의 완료 조건으로 두지 않는다.
